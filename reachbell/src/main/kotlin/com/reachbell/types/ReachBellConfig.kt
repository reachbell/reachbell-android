package com.reachbell.types

internal data class ReachBellConfig(
    val apiKey: String,
    val apiUrl: String,
    val device: DeviceInfo = DeviceInfo(),
)
