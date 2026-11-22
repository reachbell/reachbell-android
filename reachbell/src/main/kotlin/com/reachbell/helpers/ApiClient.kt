package com.reachbell.helpers

import com.reachbell.ReachBellException
import com.reachbell.constants.Headers
import com.reachbell.types.HttpResponse
import com.reachbell.types.HttpTransport
import com.reachbell.types.ReachBellConfig
import com.reachbell.utils.optNonBlankString
import com.reachbell.utils.parseJsonObject
import com.reachbell.utils.userAgent
import org.json.JSONObject
import java.io.IOException

internal class ApiClient(
    private val config: ReachBellConfig,
    private val transport: HttpTransport,
) {
    private val headers: Map<String, String> = mapOf(
        Headers.API_KEY to config.apiKey,
        Headers.CONTENT_TYPE to Headers.JSON,
        Headers.ACCEPT to "application/json",
        Headers.USER_AGENT to userAgent(config.device),
    )

    fun post(path: String, body: JSONObject): JSONObject {
        val response = send(path, body)
        val json = parseJsonObject(response.body)
        if (response.statusCode !in 200..299) {
            throw ReachBellException(
                code = ReachBellException.Code.SERVER_ERROR,
                message = serverMessage(json) ?: "ReachBell API responded with HTTP ${response.statusCode}",
                statusCode = response.statusCode,
            )
        }
        return json
    }

    fun postAction(path: String, body: JSONObject): JSONObject {
        val json = post(path, body)
        if (json.has("success") && !json.optBoolean("success", true)) {
            val reason = json.optNonBlankString("reason")
            throw ReachBellException(
                code = ReachBellException.Code.REJECTED,
                message = "ReachBell API rejected $path" + (reason?.let { ": $it" } ?: ""),
                reason = reason,
            )
        }
        return json
    }

    private fun send(path: String, body: JSONObject): HttpResponse =
        try {
            transport.post(config.apiUrl + path, headers, body.toString())
        } catch (e: IOException) {
            throw ReachBellException(
                code = ReachBellException.Code.NETWORK_ERROR,
                message = "ReachBell request to $path failed: ${e.message ?: e.javaClass.simpleName}",
                cause = e,
            )
        }

    private fun serverMessage(json: JSONObject): String? {
        val message = json.opt("message")
        if (message is org.json.JSONArray && message.length() > 0) {
            return (0 until message.length()).joinToString("; ") { message.optString(it) }
        }
        return json.optNonBlankString("message") ?: json.optNonBlankString("error")
    }
}
