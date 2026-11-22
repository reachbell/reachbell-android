package com.reachbell

import android.content.Context
import com.reachbell.constants.Defaults
import com.reachbell.helpers.AndroidDeviceInfo
import com.reachbell.helpers.FcmTokenProvider
import com.reachbell.helpers.PrefsSubscriberStore
import com.reachbell.helpers.UrlConnectionTransport
import com.reachbell.types.DeviceInfo
import com.reachbell.types.HttpTransport
import com.reachbell.types.ReachBellConfig
import com.reachbell.types.SubscriberStore
import com.reachbell.types.TokenProvider
import com.reachbell.utils.normalizeBaseUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

public object ReachBell {

    @Volatile
    private var client: ReachBellClient? = null

    @JvmStatic
    @JvmOverloads
    public fun init(context: Context, apiKey: String, apiUrl: String = Defaults.API_URL) {
        val appContext = context.applicationContext ?: context
        configure(
            apiKey = apiKey,
            apiUrl = apiUrl,
            store = PrefsSubscriberStore(appContext),
            tokenProvider = FcmTokenProvider(),
            transport = UrlConnectionTransport(),
            device = AndroidDeviceInfo.from(appContext),
        )
    }

    public suspend fun register(externalId: String? = null): String = requireClient().register(externalId)

    @JvmStatic
    public fun onNewToken(token: String) {
        client?.onNewToken(token)
    }

    public suspend fun identify(externalId: String) {
        requireClient().identify(externalId)
    }

    public suspend fun tag(tags: List<String>) {
        requireClient().tag(tags)
    }

    public suspend fun untag(tags: List<String>) {
        requireClient().untag(tags)
    }

    @JvmStatic
    public fun getSubscriberToken(): String? = client?.subscriberToken()

    /**
     * Logs the device out: unlinks the external id on the server (`POST /subscribers/unidentify`, best
     * effort) and then forgets the stored subscriber token and external id. Returns immediately; SDK calls
     * made afterwards wait for it, and [getSubscriberToken] returns null straight away.
     */
    @JvmStatic
    public fun reset() {
        client?.reset()
    }

    internal fun configure(
        apiKey: String,
        apiUrl: String = Defaults.API_URL,
        store: SubscriberStore,
        tokenProvider: TokenProvider,
        transport: HttpTransport,
        device: DeviceInfo = DeviceInfo(),
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
    ) {
        require(apiKey.isNotBlank()) { "ReachBell apiKey must not be blank" }
        client = ReachBellClient(
            config = ReachBellConfig(apiKey = apiKey.trim(), apiUrl = normalizeBaseUrl(apiUrl), device = device),
            store = store,
            tokenProvider = tokenProvider,
            transport = transport,
            ioDispatcher = ioDispatcher,
            scope = scope,
        )
    }

    internal fun clear() {
        client = null
    }

    private fun requireClient(): ReachBellClient =
        client ?: throw ReachBellException(
            ReachBellException.Code.NOT_INITIALIZED,
            "ReachBell.init() must be called before using the SDK",
        )
}
