package com.reachbell

import com.reachbell.constants.ApiPaths
import com.reachbell.helpers.ApiClient
import com.reachbell.helpers.RequestBodies
import com.reachbell.types.HttpTransport
import com.reachbell.types.ReachBellConfig
import com.reachbell.types.SubscriberStore
import com.reachbell.types.TokenProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ReachBellClient(
    config: ReachBellConfig,
    private val store: SubscriberStore,
    private val tokenProvider: TokenProvider,
    transport: HttpTransport,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope,
) {
    private val api = ApiClient(config, transport)
    private val registration = Mutex()

    suspend fun register(externalId: String?): String =
        registration.withLock {
            val fcmToken = tokenProvider.fetchToken()
            withContext(ioDispatcher) {
                val linkedId = externalId ?: store.externalId
                post(fcmToken, linkedId)
            }
        }

    fun onNewToken(token: String) {
        if (token.isBlank()) return
        scope.launch {
            runCatching {
                registration.withLock {
                    // Checked under the lock so a register() still in flight (token fetch included)
                    // finishes first; otherwise the rotated token is dropped and a stale one is stored.
                    val current = store.token ?: return@launch
                    if (current == token) return@launch
                    post(token, store.externalId)
                }
            }
        }
    }

    suspend fun identify(externalId: String) {
        withContext(ioDispatcher) {
            val token = requireToken()
            api.postAction(ApiPaths.IDENTIFY, RequestBodies.identify(token, externalId))
            store.externalId = externalId
        }
    }

    suspend fun tag(tags: List<String>) {
        withContext(ioDispatcher) {
            api.postAction(ApiPaths.TAG, RequestBodies.tags(requireToken(), tags))
        }
    }

    suspend fun untag(tags: List<String>) {
        withContext(ioDispatcher) {
            api.postAction(ApiPaths.UNTAG, RequestBodies.tags(requireToken(), tags))
        }
    }

    fun subscriberToken(): String? = store.token

    fun reset() {
        store.clear()
    }

    private fun post(fcmToken: String, externalId: String?): String {
        val response = api.post(ApiPaths.SUBSCRIBERS, RequestBodies.subscribe(fcmToken, externalId))
        val subscriberToken = response.optString("token").takeIf { it.isNotBlank() } ?: fcmToken
        store.save(subscriberToken, externalId)
        return subscriberToken
    }

    private fun requireToken(): String =
        store.token ?: throw ReachBellException(
            ReachBellException.Code.NO_SUBSCRIPTION,
            "ReachBell.register() must succeed before identify(), tag() or untag()",
        )
}
