package com.reachbell.helpers

import com.reachbell.constants.Defaults
import com.reachbell.types.HttpResponse
import com.reachbell.types.HttpTransport
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal class UrlConnectionTransport(
    private val connectTimeoutMs: Int = Defaults.CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = Defaults.READ_TIMEOUT_MS,
) : HttpTransport {

    @Throws(IOException::class)
    override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.doOutput = true
            connection.useCaches = false
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val payload = body.toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            return HttpResponse(status, readFully(stream))
        } finally {
            connection.disconnect()
        }
    }

    private fun readFully(stream: InputStream?): String =
        stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
}
