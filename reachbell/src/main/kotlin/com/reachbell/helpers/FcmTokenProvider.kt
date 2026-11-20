package com.reachbell.helpers

import com.google.firebase.messaging.FirebaseMessaging
import com.reachbell.ReachBellException
import com.reachbell.types.TokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FcmTokenProvider : TokenProvider {

    override suspend fun fetchToken(): String {
        val messaging = try {
            FirebaseMessaging.getInstance()
        } catch (e: Exception) {
            throw unavailable("Firebase is not initialized; check google-services.json", e)
        }
        return suspendCancellableCoroutine { continuation ->
            try {
                messaging.token.addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    val token = if (task.isSuccessful) task.result else null
                    if (!token.isNullOrBlank()) {
                        continuation.resume(token)
                    } else {
                        continuation.resumeWithException(
                            unavailable("Firebase could not return an FCM token", task.exception),
                        )
                    }
                }
            } catch (e: Exception) {
                continuation.resumeWithException(unavailable("Firebase could not return an FCM token", e))
            }
        }
    }

    private fun unavailable(message: String, cause: Throwable?): ReachBellException =
        ReachBellException(
            code = ReachBellException.Code.FCM_TOKEN_UNAVAILABLE,
            message = message + (cause?.message?.let { ": $it" } ?: ""),
            cause = cause,
        )
}
