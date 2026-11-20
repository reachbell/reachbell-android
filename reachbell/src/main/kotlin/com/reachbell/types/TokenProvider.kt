package com.reachbell.types

internal fun interface TokenProvider {
    suspend fun fetchToken(): String
}
