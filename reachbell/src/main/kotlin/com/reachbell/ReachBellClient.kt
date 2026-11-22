package com.reachbell

import com.reachbell.constants.ApiPaths
import com.reachbell.constants.Defaults
import com.reachbell.helpers.ApiClient
import com.reachbell.helpers.RequestBodies
import com.reachbell.types.HttpTransport
import com.reachbell.types.ReachBellConfig
import com.reachbell.types.SubscriberStore
import com.reachbell.types.TokenProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal class ReachBellClient(
    private val config: ReachBellConfig,
    private val store: SubscriberStore,
    private val tokenProvider: TokenProvider,
    transport: HttpTransport,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope,
    private val tokenFetchTimeoutMs: Long = Defaults.TOKEN_FETCH_TIMEOUT_MS,
) {
    private val api = ApiClient(config, transport)

    /**
     * Serialises every read-modify-write of the stored subscription (register, onNewToken, identify,
     * tag, untag, reset). It is never held across the FCM token fetch, only across the API call.
     */
    private val lock = Mutex()

    /** A token handed to [onNewToken]. Compared by identity so the same token string pushed twice still counts. */
    private class PushedToken(val value: String)

    private val latestPushedToken = AtomicReference<PushedToken?>(null)

    /** Bumped synchronously by [reset] so a register() that started earlier does not re-save its result. */
    private val resetEpoch = AtomicInteger()

    @Volatile
    private var pendingReset: Job? = null

    /** In-memory copy of the stored subscriber token, so getSubscriberToken() never touches disk. */
    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cacheLoaded = false

    init {
        scope.launch(ioDispatcher) {
            lock.withLock { loadCache() }
        }
    }

    suspend fun register(externalId: String?): String {
        // Epoch first: reset() publishes its job before bumping, so either we see the bump here and
        // wait for the published job, or we read the old epoch and back off at the lock check below.
        val epoch = resetEpoch.get()
        awaitPendingReset()
        val pushedBefore = latestPushedToken.get()
        // Outside the lock: a hung Firebase task must not block onNewToken() or other calls.
        val fetched = fetchToken()
        return lock.withLock {
            if (resetEpoch.get() != epoch) {
                throw ReachBellException(
                    ReachBellException.Code.NO_SUBSCRIPTION,
                    "ReachBell.reset() was called while register() was in flight",
                )
            }
            // If onNewToken() delivered a rotated token while we were fetching, the fetched one may be
            // stale. Checked under the lock, so the rotated token is never dropped.
            val pushedNow = latestPushedToken.get()
            val fcmToken = if (pushedNow != null && pushedNow !== pushedBefore) pushedNow.value else fetched
            withContext(ioDispatcher) {
                // C1a: after reset(), the first registration without an externalId clears it server-side.
                val clearExternalId = externalId.isNullOrBlank() && store.externalIdClearPending
                post(fcmToken, externalId ?: store.externalId, clearExternalId)
            }
        }
    }

    fun onNewToken(token: String) {
        if (token.isBlank()) return
        val pushed = PushedToken(token)
        latestPushedToken.set(pushed)
        scope.launch {
            try {
                awaitPendingReset()
                lock.withLock {
                    // A newer rotation arrived; that one will be registered instead.
                    if (latestPushedToken.get() !== pushed) return@withLock
                    withContext(ioDispatcher) {
                        // No subscription yet: a register() in flight picks the pushed token up itself.
                        val current = store.token
                        if (current != null && current != token) {
                            post(token, store.externalId)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Best effort: the next register() call retries with the current token.
            }
        }
    }

    suspend fun identify(externalId: String) {
        awaitPendingReset()
        lock.withLock {
            withContext(ioDispatcher) {
                val token = requireToken()
                api.postAction(ApiPaths.IDENTIFY, RequestBodies.identify(token, externalId))
                store.externalId = externalId
            }
        }
    }

    suspend fun tag(tags: List<String>) {
        awaitPendingReset()
        lock.withLock {
            withContext(ioDispatcher) {
                api.postAction(ApiPaths.TAG, RequestBodies.tags(requireToken(), tags))
            }
        }
    }

    suspend fun untag(tags: List<String>) {
        awaitPendingReset()
        lock.withLock {
            withContext(ioDispatcher) {
                api.postAction(ApiPaths.UNTAG, RequestBodies.tags(requireToken(), tags))
            }
        }
    }

    /** Never reads SharedPreferences on the caller thread once the background warm-up has run. */
    fun subscriberToken(): String? {
        if (pendingReset?.isActive == true) return null
        return if (cacheLoaded) cachedToken else store.token
    }

    /**
     * Returns immediately. In the background it unlinks the external id from the stored subscriber
     * (`POST /subscribers/unidentify`, best effort) and then forgets the local subscription. SDK calls
     * made after reset() wait for it; a register() still fetching its FCM token when reset() is called
     * fails with NO_SUBSCRIPTION instead of re-saving the cleared subscription.
     */
    @Synchronized
    fun reset(): Job {
        val previous = pendingReset
        val job = scope.launch {
            previous?.join()
            lock.withLock {
                withContext(ioDispatcher) {
                    store.token?.let { token -> unidentifyBestEffort(token) }
                    store.clear()
                    cachedToken = null
                    cacheLoaded = true
                }
            }
        }
        // Published before the epoch bump: a register() either sees the job and waits for it, or sees
        // the bump at its lock check and backs off.
        pendingReset = job
        resetEpoch.incrementAndGet()
        return job
    }

    private fun unidentifyBestEffort(token: String) {
        try {
            api.postAction(ApiPaths.UNIDENTIFY, RequestBodies.unidentify(token))
        } catch (e: Exception) {
            // Offline or rejected: local state is still cleared so the next user starts clean.
        }
    }

    private suspend fun awaitPendingReset() {
        pendingReset?.join()
    }

    private suspend fun fetchToken(): String =
        withTimeoutOrNull(tokenFetchTimeoutMs) { tokenProvider.fetchToken() }
            ?: throw ReachBellException(
                ReachBellException.Code.FCM_TOKEN_UNAVAILABLE,
                "Firebase did not return an FCM token within ${tokenFetchTimeoutMs / 1000}s",
            )

    private fun loadCache() {
        if (cacheLoaded) return
        cachedToken = store.token
        cacheLoaded = true
    }

    private fun post(fcmToken: String, externalId: String?, clearExternalId: Boolean = false): String {
        // C2: on rotation, the API moves tags and externalId from the previous subscriber to this one.
        val previousToken = store.token?.takeIf { it != fcmToken }
        val response = api.post(
            ApiPaths.SUBSCRIBERS,
            RequestBodies.subscribe(fcmToken, externalId, previousToken, config.device, clearExternalId),
        )
        val subscriberToken = response.optString("token").takeIf { it.isNotBlank() } ?: fcmToken
        store.save(subscriberToken, externalId)
        cachedToken = subscriberToken
        cacheLoaded = true
        return subscriberToken
    }

    private fun requireToken(): String =
        store.token ?: throw ReachBellException(
            ReachBellException.Code.NO_SUBSCRIPTION,
            "ReachBell.register() must succeed before identify(), tag() or untag()",
        )
}
