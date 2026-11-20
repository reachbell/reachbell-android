package com.reachbell.constants

internal object Defaults {
    const val API_URL = "https://api.reachbell.com"
    const val SDK_VERSION = "1.0.0"
    const val USER_AGENT = "reachbell-android/$SDK_VERSION"
    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 15_000
    const val TOKEN_TYPE = "fcm"
    const val PLATFORM = "android"
}
