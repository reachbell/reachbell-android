package com.reachbell.helpers

import android.content.Context
import android.os.Build
import com.reachbell.types.DeviceInfo

internal object AndroidDeviceInfo {

    private const val TABLET_MIN_SMALLEST_WIDTH_DP = 600

    fun from(context: Context): DeviceInfo {
        val smallestWidthDp = runCatching { context.resources.configuration.smallestScreenWidthDp }.getOrNull()
        return DeviceInfo(
            osVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            deviceType = when {
                smallestWidthDp == null || smallestWidthDp <= 0 -> null
                smallestWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP -> "tablet"
                else -> "mobile"
            },
        )
    }
}
