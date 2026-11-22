package com.reachbell.helpers

import com.reachbell.ReachBellException
import com.reachbell.types.TokenProvider
import kotlinx.coroutines.CompletableDeferred

internal class FakeTokenProvider(var token: String? = "fcm-token-1") : TokenProvider {

    var calls = 0
        private set

    /** When set, fetchToken() suspends until it completes (never completing it simulates a hung task). */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun fetchToken(): String {
        calls++
        gate?.await()
        return token ?: throw ReachBellException(
            ReachBellException.Code.FCM_TOKEN_UNAVAILABLE,
            "no token",
        )
    }
}
