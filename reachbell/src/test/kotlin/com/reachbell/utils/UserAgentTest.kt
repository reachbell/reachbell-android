package com.reachbell.utils

import com.reachbell.constants.Defaults
import com.reachbell.types.DeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class UserAgentTest {

    private val v = Defaults.SDK_VERSION

    @Test
    fun includesAndroidReleaseAndModel() {
        assertEquals(
            "ReachBell/$v (Linux; Android 14; Pixel 8) reachbell-android/$v",
            userAgent(DeviceInfo(osVersion = "14", model = "Pixel 8")),
        )
    }

    @Test
    fun omitsUnknownParts() {
        assertEquals("ReachBell/$v (Linux; Android) reachbell-android/$v", userAgent(DeviceInfo()))
    }

    @Test
    fun stripsSeparatorsAndNonAsciiFromModel() {
        assertEquals(
            "ReachBell/$v (Linux; Android 13; Foo Bar Baz) reachbell-android/$v",
            userAgent(DeviceInfo(osVersion = "13", model = "Foo;Bar\nBazé")),
        )
    }
}
