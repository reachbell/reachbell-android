package com.reachbell.helpers

import com.reachbell.ReachBellException
import com.reachbell.types.TokenProvider

internal class FakeTokenProvider(var token: String? = "fcm-token-1") : TokenProvider {

    var calls = 0
        private set

    override suspend fun fetchToken(): String {
        calls++
        return token ?: throw ReachBellException(
            ReachBellException.Code.FCM_TOKEN_UNAVAILABLE,
            "no token",
        )
    }
}
