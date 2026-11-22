package com.reachbell.utils

import com.reachbell.constants.Defaults
import com.reachbell.types.DeviceInfo

/**
 * `ReachBell/<ver> (Linux; Android <release>; <model>) reachbell-android/<ver>`.
 *
 * The leading product token deliberately does not contain "android": UAParser matches the first
 * `android[/ ]<version>` in the string, so `reachbell-android/1.0.0 (...)` would be recorded as
 * Android 1.0.0. The `(Linux; Android x; model)` comment is the shape UAParser reads for os and device.
 */
internal fun userAgent(device: DeviceInfo): String {
    val parts = mutableListOf("Linux")
    parts += listOfNotNull(Defaults.OS_NAME, sanitize(device.osVersion)).joinToString(" ")
    sanitize(device.model)?.let { parts += it }
    return "ReachBell/${Defaults.SDK_VERSION} (${parts.joinToString("; ")}) ${Defaults.SDK_NAME}/${Defaults.SDK_VERSION}"
}

/** Header-safe: printable ASCII only, no `;` (it separates the comment's fields). */
private fun sanitize(value: String?): String? =
    value
        ?.map { c -> if (c in ' '..'~' && c != ';') c else ' ' }
        ?.joinToString("")
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
