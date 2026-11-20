package com.reachbell

public class ReachBellException internal constructor(
    public val code: Code,
    message: String,
    public val statusCode: Int? = null,
    public val reason: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    public enum class Code(public val value: String) {
        NOT_INITIALIZED("not_initialized"),
        NO_SUBSCRIPTION("no_subscription"),
        FCM_TOKEN_UNAVAILABLE("fcm_token_unavailable"),
        NETWORK_ERROR("network_error"),
        SERVER_ERROR("server_error"),
        REJECTED("rejected"),
    }
}
