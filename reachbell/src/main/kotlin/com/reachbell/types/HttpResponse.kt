package com.reachbell.types

internal data class HttpResponse(
    val statusCode: Int,
    val body: String,
)
