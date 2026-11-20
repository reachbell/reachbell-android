package com.reachbell.helpers

import com.reachbell.types.HttpResponse
import com.reachbell.types.HttpTransport
import org.json.JSONObject
import java.io.IOException

internal class FakeTransport : HttpTransport {

    data class Request(val url: String, val headers: Map<String, String>, val body: JSONObject)

    val requests = mutableListOf<Request>()
    private val responses = ArrayDeque<() -> HttpResponse>()

    fun respond(statusCode: Int, body: String) {
        responses.addLast { HttpResponse(statusCode, body) }
    }

    fun fail(error: IOException) {
        responses.addLast { throw error }
    }

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
        requests += Request(url, headers, JSONObject(body))
        val next = responses.removeFirstOrNull() ?: return HttpResponse(200, "{}")
        return next()
    }
}
