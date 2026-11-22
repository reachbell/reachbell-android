package com.reachbell.types

/**
 * What the SDK tells the API about the device. The API parses the User-Agent with UAParser and falls
 * back to the `os` / `osVersion` / `device` body fields, so both are sent.
 */
internal data class DeviceInfo(
    val osVersion: String? = null,
    val model: String? = null,
    /** `mobile` or `tablet`, the values UAParser uses. */
    val deviceType: String? = null,
)
