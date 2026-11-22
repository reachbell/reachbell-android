package com.reachbell

import com.reachbell.helpers.FakeSubscriberStore
import com.reachbell.helpers.FakeTokenProvider
import com.reachbell.helpers.FakeTransport
import com.reachbell.types.DeviceInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ReachBellTest {

    private lateinit var store: FakeSubscriberStore
    private lateinit var tokens: FakeTokenProvider
    private lateinit var transport: FakeTransport

    @Before
    fun setUp() {
        ReachBell.clear()
        store = FakeSubscriberStore()
        tokens = FakeTokenProvider()
        transport = FakeTransport()
    }

    @After
    fun tearDown() {
        ReachBell.clear()
    }

    private fun TestScope.configure(apiUrl: String = "https://api.example.test/") {
        val dispatcher = StandardTestDispatcher(testScheduler)
        ReachBell.configure(
            apiKey = "rb_live_test",
            apiUrl = apiUrl,
            store = store,
            tokenProvider = tokens,
            transport = transport,
            device = DeviceInfo(osVersion = "14", model = "Pixel 8", deviceType = "mobile"),
            ioDispatcher = dispatcher,
            scope = this,
        )
    }

    private suspend fun expectCode(code: ReachBellException.Code, block: suspend () -> Unit): ReachBellException {
        try {
            block()
        } catch (e: ReachBellException) {
            assertEquals(code, e.code)
            return e
        }
        fail("expected ReachBellException with $code")
        throw AssertionError()
    }

    @Test
    fun registerPostsSubscriberBodyAndHeaders() = runTest {
        configure()
        transport.respond(201, """{"_id":"abc","token":"fcm-token-1","platform":"android"}""")

        val token = ReachBell.register()

        assertEquals("fcm-token-1", token)
        assertEquals(1, transport.requests.size)
        val request = transport.requests.single()
        assertEquals("https://api.example.test/subscribers", request.url)
        assertEquals("rb_live_test", request.headers["x-api-key"])
        assertEquals("fcm-token-1", request.body.getString("token"))
        assertEquals("fcm", request.body.getString("tokenType"))
        assertEquals("android", request.body.getString("platform"))
        assertFalse(request.body.has("externalId"))
        assertFalse(request.body.has("previousToken"))
        assertEquals("Android", request.body.getString("os"))
        assertEquals("14", request.body.getString("osVersion"))
        assertEquals("mobile", request.body.getString("device"))
        assertEquals("ReachBell/1.0.0 (Linux; Android 14; Pixel 8) reachbell-android/1.0.0", request.headers["User-Agent"])
        assertEquals("fcm-token-1", ReachBell.getSubscriberToken())
    }

    @Test
    fun registerWithExternalIdSendsAndPersistsIt() = runTest {
        configure()

        ReachBell.register(externalId = "user_42")

        assertEquals("user_42", transport.requests.single().body.getString("externalId"))
        assertEquals("user_42", store.externalId)
    }

    @Test
    fun registerWithoutExternalIdReusesStoredOne() = runTest {
        configure()
        ReachBell.register(externalId = "user_42")
        tokens.token = "fcm-token-2"

        val token = ReachBell.register()

        assertEquals("fcm-token-2", token)
        assertEquals("user_42", transport.requests.last().body.getString("externalId"))
        assertEquals("fcm-token-1", transport.requests.last().body.getString("previousToken"))
        assertEquals("user_42", store.externalId)
    }

    @Test
    fun registerFallsBackToFcmTokenWhenResponseHasNoToken() = runTest {
        configure()
        transport.respond(200, "")

        assertEquals("fcm-token-1", ReachBell.register())
    }

    @Test
    fun identifyPostsAndPersistsExternalId() = runTest {
        configure()
        ReachBell.register()
        transport.respond(200, """{"success":true,"updated":true}""")

        ReachBell.identify("user_7")

        val request = transport.requests.last()
        assertEquals("https://api.example.test/subscribers/identify", request.url)
        assertEquals("fcm-token-1", request.body.getString("token"))
        assertEquals("user_7", request.body.getString("externalId"))
        assertEquals("user_7", store.externalId)
    }

    @Test
    fun tagAndUntagPostTags() = runTest {
        configure()
        ReachBell.register()
        transport.respond(200, """{"success":true,"updated":true}""")
        transport.respond(200, """{"success":true,"updated":true}""")

        ReachBell.tag(listOf("pro", "weekly_digest"))
        ReachBell.untag(listOf("pro"))

        val tag = transport.requests[1]
        assertEquals("https://api.example.test/subscribers/tag", tag.url)
        assertEquals("fcm-token-1", tag.body.getString("token"))
        assertEquals(listOf("pro", "weekly_digest"), tag.body.getJSONArray("tags").let { a -> (0 until a.length()).map { a.getString(it) } })
        val untag = transport.requests[2]
        assertEquals("https://api.example.test/subscribers/untag", untag.url)
        assertEquals("pro", untag.body.getJSONArray("tags").getString(0))
    }

    @Test
    fun identifyTagUntagBeforeRegisterThrowNoSubscription() = runTest {
        configure()

        expectCode(ReachBellException.Code.NO_SUBSCRIPTION) { ReachBell.identify("user_1") }
        expectCode(ReachBellException.Code.NO_SUBSCRIPTION) { ReachBell.tag(listOf("a")) }
        expectCode(ReachBellException.Code.NO_SUBSCRIPTION) { ReachBell.untag(listOf("a")) }
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun callsBeforeInitThrowNotInitialized() = runTest {
        expectCode(ReachBellException.Code.NOT_INITIALIZED) { ReachBell.register() }
        expectCode(ReachBellException.Code.NOT_INITIALIZED) { ReachBell.identify("user_1") }
        expectCode(ReachBellException.Code.NOT_INITIALIZED) { ReachBell.tag(listOf("a")) }
        expectCode(ReachBellException.Code.NOT_INITIALIZED) { ReachBell.untag(listOf("a")) }
        assertNull(ReachBell.getSubscriberToken())
        ReachBell.reset()
    }

    @Test
    fun non2xxThrowsServerErrorWithStatusCode() = runTest {
        configure()
        transport.respond(403, """{"statusCode":403,"message":"Push subscriber limit reached for your plan. Upgrade to add more."}""")

        val error = expectCode(ReachBellException.Code.SERVER_ERROR) { ReachBell.register() }

        assertEquals(403, error.statusCode)
        assertEquals("Push subscriber limit reached for your plan. Upgrade to add more.", error.message)
        assertNull(ReachBell.getSubscriberToken())
    }

    @Test
    fun non2xxWithoutJsonStillCarriesStatusCode() = runTest {
        configure()
        ReachBell.register()
        transport.respond(502, "<html>Bad gateway</html>")

        val error = expectCode(ReachBellException.Code.SERVER_ERROR) { ReachBell.tag(listOf("a")) }

        assertEquals(502, error.statusCode)
    }

    @Test
    fun successFalseThrowsRejectedWithReason() = runTest {
        configure()
        ReachBell.register()
        transport.respond(200, """{"success":false,"updated":false,"reason":"subscriber_not_found"}""")

        val error = expectCode(ReachBellException.Code.REJECTED) { ReachBell.identify("user_1") }

        assertEquals("subscriber_not_found", error.reason)
        assertNull(store.externalId)
    }

    @Test
    fun transportFailureThrowsNetworkError() = runTest {
        configure()
        transport.fail(IOException("timeout"))

        val error = expectCode(ReachBellException.Code.NETWORK_ERROR) { ReachBell.register() }

        assertTrue(error.cause is IOException)
        assertNull(error.statusCode)
    }

    @Test
    fun tokenUnavailableThrowsFcmTokenUnavailable() = runTest {
        configure()
        tokens.token = null

        expectCode(ReachBellException.Code.FCM_TOKEN_UNAVAILABLE) { ReachBell.register() }

        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun onNewTokenReRegistersKeepingExternalId() = runTest {
        configure()
        ReachBell.register(externalId = "user_42")

        ReachBell.onNewToken("fcm-token-rotated")
        advanceUntilIdle()

        assertEquals(2, transport.requests.size)
        val request = transport.requests.last()
        assertEquals("https://api.example.test/subscribers", request.url)
        assertEquals("fcm-token-rotated", request.body.getString("token"))
        assertEquals("user_42", request.body.getString("externalId"))
        assertEquals("fcm-token-1", request.body.getString("previousToken"))
        assertEquals("fcm-token-rotated", ReachBell.getSubscriberToken())
        assertEquals("user_42", store.externalId)
    }

    @Test
    fun onNewTokenWithSameTokenDoesNothing() = runTest {
        configure()
        ReachBell.register()

        ReachBell.onNewToken("fcm-token-1")
        advanceUntilIdle()

        assertEquals(1, transport.requests.size)
    }

    @Test
    fun onNewTokenBeforeInitIsNoOp() = runTest {
        ReachBell.onNewToken("fcm-token-rotated")
        advanceUntilIdle()

        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun onNewTokenWithoutPriorRegistrationIsNoOp() = runTest {
        configure()

        ReachBell.onNewToken("fcm-token-rotated")
        advanceUntilIdle()

        assertTrue(transport.requests.isEmpty())
        assertNull(ReachBell.getSubscriberToken())
    }

    @Test
    fun onNewTokenSwallowsFailures() = runTest {
        configure()
        ReachBell.register()
        transport.fail(IOException("offline"))

        ReachBell.onNewToken("fcm-token-rotated")
        advanceUntilIdle()

        assertEquals("fcm-token-1", ReachBell.getSubscriberToken())
    }

    @Test
    fun resetUnidentifiesThenForgetsTokenAndExternalId() = runTest {
        configure()
        ReachBell.register(externalId = "user_42")
        transport.respond(200, """{"success":true}""")

        ReachBell.reset()
        assertNull(ReachBell.getSubscriberToken())
        advanceUntilIdle()

        val request = transport.requests.last()
        assertEquals("https://api.example.test/subscribers/unidentify", request.url)
        assertEquals("fcm-token-1", request.body.getString("token"))
        assertNull(ReachBell.getSubscriberToken())
        assertNull(store.token)
        assertNull(store.externalId)
        expectCode(ReachBellException.Code.NO_SUBSCRIPTION) { ReachBell.identify("user_42") }
    }

    @Test
    fun resetClearsLocalStateEvenWhenUnidentifyFails() = runTest {
        configure()
        ReachBell.register(externalId = "user_42")
        transport.fail(IOException("offline"))

        ReachBell.reset()
        advanceUntilIdle()

        assertEquals(2, transport.requests.size)
        assertNull(store.token)
        assertNull(store.externalId)
    }

    @Test
    fun resetWithoutSubscriptionSendsNothing() = runTest {
        configure()

        ReachBell.reset()
        advanceUntilIdle()

        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun callsAfterResetWaitForIt() = runTest {
        configure()
        ReachBell.register(externalId = "user_42")

        ReachBell.reset()
        ReachBell.register(externalId = "user_9")

        assertEquals(
            listOf("/subscribers", "/subscribers/unidentify", "/subscribers"),
            transport.requests.map { it.url.removePrefix("https://api.example.test") },
        )
        val body = transport.requests.last().body
        assertEquals("user_9", body.getString("externalId"))
        assertFalse(body.has("previousToken"))
        assertEquals("user_9", store.externalId)
    }

    @Test
    fun resetDuringInFlightRegisterDoesNotResaveSubscription() = runTest {
        configure()
        val gate = CompletableDeferred<Unit>()
        tokens.gate = gate
        val pending = async { runCatching { ReachBell.register(externalId = "user_42") } }
        runCurrent()

        ReachBell.reset()
        gate.complete(Unit)
        advanceUntilIdle()

        val error = pending.await().exceptionOrNull() as ReachBellException
        assertEquals(ReachBellException.Code.NO_SUBSCRIPTION, error.code)
        assertTrue(transport.requests.isEmpty())
        assertNull(store.token)
        assertNull(store.externalId)
    }

    @Test
    fun tokenRotatedDuringRegisterFetchIsTheOneRegistered() = runTest {
        configure()
        val gate = CompletableDeferred<Unit>()
        tokens.gate = gate
        val pending = async { ReachBell.register() }
        runCurrent()

        ReachBell.onNewToken("fcm-token-rotated")
        runCurrent()
        gate.complete(Unit)

        assertEquals("fcm-token-rotated", pending.await())
        advanceUntilIdle()
        assertEquals(1, transport.requests.size)
        assertEquals("fcm-token-rotated", transport.requests.single().body.getString("token"))
        assertEquals("fcm-token-rotated", store.token)
    }

    @Test
    fun hungTokenFetchTimesOutWithoutBlockingRotation() = runTest {
        configure()
        ReachBell.register()
        tokens.gate = CompletableDeferred() // never completes
        val pending = async { runCatching { ReachBell.register() } }
        runCurrent()

        ReachBell.onNewToken("fcm-token-rotated")
        runCurrent()

        assertEquals(2, transport.requests.size)
        assertEquals("fcm-token-rotated", transport.requests.last().body.getString("token"))
        assertEquals("fcm-token-1", transport.requests.last().body.getString("previousToken"))

        advanceUntilIdle()
        val error = pending.await().exceptionOrNull() as ReachBellException
        assertEquals(ReachBellException.Code.FCM_TOKEN_UNAVAILABLE, error.code)
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun codesExposeDocumentedWireValues() {
        assertEquals("not_initialized", ReachBellException.Code.NOT_INITIALIZED.value)
        assertEquals("no_subscription", ReachBellException.Code.NO_SUBSCRIPTION.value)
        assertEquals("fcm_token_unavailable", ReachBellException.Code.FCM_TOKEN_UNAVAILABLE.value)
        assertEquals("network_error", ReachBellException.Code.NETWORK_ERROR.value)
        assertEquals("server_error", ReachBellException.Code.SERVER_ERROR.value)
        assertEquals("rejected", ReachBellException.Code.REJECTED.value)
    }
}
