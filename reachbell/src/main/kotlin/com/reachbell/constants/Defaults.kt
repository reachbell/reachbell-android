package com.reachbell.constants

internal object Defaults {
    const val API_URL = "https://api.reachbell.com"
    const val SDK_VERSION = "1.0.0"
    const val SDK_NAME = "reachbell-android"
    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 15_000
    /** Upper bound on FirebaseMessaging.getToken(); a hung task must not stall the SDK. */
    const val TOKEN_FETCH_TIMEOUT_MS = 30_000L
    const val TOKEN_TYPE = "fcm"
    const val PLATFORM = "android"
    const val OS_NAME = "Android"
}
