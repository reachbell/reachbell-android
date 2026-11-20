package com.reachbell.utils

import org.json.JSONException
import org.json.JSONObject

internal fun parseJsonObject(body: String): JSONObject {
    if (body.isBlank()) return JSONObject()
    return try {
        JSONObject(body)
    } catch (e: JSONException) {
        JSONObject()
    }
}

internal fun JSONObject.optNonBlankString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key)
    return when (value) {
        is String -> value.takeIf { it.isNotBlank() }
        else -> value?.toString()?.takeIf { it.isNotBlank() }
    }
}
