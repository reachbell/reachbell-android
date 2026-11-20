package com.reachbell.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun trimsWhitespaceAndTrailingSlashes() {
        assertEquals("https://api.reachbell.com", normalizeBaseUrl(" https://api.reachbell.com// "))
        assertEquals("http://localhost:3000", normalizeBaseUrl("http://localhost:3000"))
    }
}
