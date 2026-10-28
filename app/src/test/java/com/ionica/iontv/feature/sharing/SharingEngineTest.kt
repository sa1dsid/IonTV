package com.ionica.iontv.feature.sharing

import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.core.network.Connection
import com.ionica.iontv.core.network.FakeNetworkMonitor
import com.ionica.iontv.core.network.NetworkStatus
import com.ionica.iontv.core.session.SessionTokens
import com.ionica.iontv.core.system.FakeBackgroundPermissions
import com.ionica.iontv.feature.sharing.data.HeartbeatRepository
import com.ionica.iontv.feature.sharing.domain.model.PauseReason
import com.ionica.iontv.feature.sharing.domain.model.SharingState
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus
import com.ionica.iontv.testing.ViewModelTestCase
import io.proxysdk.ProxyCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ACTIVE = """{"bound":true,"status":"active","reason":null,"interval_id":null}"""

private val ISO_UTC = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z""")

private val UUID = Regex("""[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")

@OptIn(ExperimentalCoroutinesApi::class)
class SharingEngineTest : ViewModelTestCase() {

    private val network = FakeNetworkMonitor()

    private val permissions = FakeBackgroundPermissions(batteryIgnored = true)

    private val proxy = FakeProxyClient()

    @Test
    fun `a linked tv reports itself right away and then every ten minutes`() = screen { stack ->
        stack.server.always(HEARTBEAT, body = ACTIVE)
        val engine = engine(stack)

        start(engine, stack)
        settle("first heartbeat") { stack.server.hits(HEARTBEAT) == 1 }

        val body = stack.server.bodyOf(HEARTBEAT)
        assertTrue(body, Regex(""""event_id":"${UUID.pattern}"""").containsMatchIn(body))
        assertTrue(body, Regex(""""timestamp":"${ISO_UTC.pattern}"""").containsMatchIn(body))
        assertTrue(body, body.contains(""""battery_ok":true,"service_ok":true,"connection":"ethernet""""))
        assertEquals(listOf("Bearer tv.access"), stack.server.authorizationsOf(HEARTBEAT))

        advanceTimeBy(9 * MINUTE)
        assertEquals(1, stack.server.hits(HEARTBEAT))
        advanceTimeBy(MINUTE + 1)
        settle("second heartbeat") { stack.server.hits(HEARTBEAT) == 2 }
    }

    @Test
    fun `a pause the backend reports for the battery reaches the dashboard`() = screen { stack ->
        stack.server.always(
            HEARTBEAT,
            body = """{"bound":true,"status":"paused","reason":"battery_optimization","interval_id":null}""",
        )
        val engine = engine(stack)
        val state = observe(engine)

        start(engine, stack)
        settle("paused") { state()?.status is SharingStatus.Paused }

        assertEquals(SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION), state()?.status)
        assertEquals(Connection.ETHERNET, state()?.connection)
    }

    @Test
    fun `consent given again is checked with the backend right away`() = screen { stack ->
        stack.server.always(
            HEARTBEAT,
            body = """{"bound":true,"status":"paused","reason":"consent_required","interval_id":null}""",
        )
        val engine = engine(stack)
        val state = observe(engine)
        start(engine, stack)
        settle("paused for consent") { state()?.status == SharingStatus.Paused(PauseReason.CONSENT_REQUIRED) }

        stack.server.always(HEARTBEAT, body = ACTIVE)
        engine.refresh()

        settle("fresh heartbeat") { stack.server.hits(HEARTBEAT) == 2 }
        settle("active") { state()?.status == SharingStatus.Active }
    }

    @Test
    fun `battery optimization switched back on pauses at once and tells the backend early`() =
        screen { stack ->
            stack.server.always(HEARTBEAT, body = ACTIVE)
            val engine = engine(stack)
            val state = observe(engine)
            start(engine, stack)
            settle("active") { state()?.status == SharingStatus.Active && stack.server.hits(HEARTBEAT) == 1 }

            permissions.batteryIgnored = false
            advanceTimeBy(31_000)
            settle("early heartbeat") { stack.server.hits(HEARTBEAT) == 2 }

            assertEquals(SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION), state()?.status)
            assertTrue(stack.server.bodiesOf(HEARTBEAT).last().contains(""""battery_ok":false"""))
        }

    @Test
    fun `a tv with nowhere to lift battery optimization still reports battery ok`() = screen { stack ->
        permissions.batteryIgnored = false
        permissions.exemptionScreen = false
        stack.server.always(HEARTBEAT, body = ACTIVE)
        val engine = engine(stack)

        start(engine, stack)
        settle("heartbeat") { stack.server.hits(HEARTBEAT) == 1 }

        assertTrue(stack.server.bodyOf(HEARTBEAT).contains(""""battery_ok":true"""))
    }

    @Test
    fun `without a network the tv is offline and reports as soon as it is back`() = screen { stack ->
        network.switchTo(NetworkStatus.Offline)
        stack.server.always(HEARTBEAT, body = ACTIVE)
        val engine = engine(stack)
        val state = observe(engine)

        start(engine, stack)
        settle("offline") { state()?.status == SharingStatus.Offline }
        advanceTimeBy(5 * MINUTE)
        assertEquals(0, stack.server.hits(HEARTBEAT))

        network.switchTo(NetworkStatus(isOnline = true, connection = Connection.WIFI))
        settle("heartbeat") { stack.server.hits(HEARTBEAT) == 1 }

        assertTrue(stack.server.bodyOf(HEARTBEAT).contains(""""connection":"wifi""""))
        settle("active") { state()?.status == SharingStatus.Active }
    }

    @Test
    fun `a heartbeat that does not reach the server shows offline and is retried in a minute`() =
        screen { stack ->
            stack.server.nextDropsConnection(HEARTBEAT)
            stack.server.always(HEARTBEAT, body = ACTIVE)
            val engine = engine(stack)
            val state = observe(engine)

            start(engine, stack)
            settle("offline") { state()?.status == SharingStatus.Offline }

            advanceTimeBy(MINUTE + 1)
            settle("retried") { state()?.status == SharingStatus.Active }
            assertEquals(2, stack.server.hits(HEARTBEAT))
        }

    @Test
    fun `an unlinked tv drops its session and the engine stops`() = screen { stack ->
        stack.server.always(HEARTBEAT, body = """{"bound":false,"status":"unbound","reason":"device_unlinked"}""")
        val engine = engine(stack)

        val run = start(engine, stack)
        settle("stopped") { run.isCompleted }

        assertNull(stack.sessionStore.current())
        assertEquals(1, stack.server.hits(HEARTBEAT))
    }

    @Test
    fun `service ok follows the proxy and its start is reported without waiting ten minutes`() =
        screen { stack ->
            proxy.serving.value = false
            stack.server.always(HEARTBEAT, body = ACTIVE)
            val engine = engine(stack)

            start(engine, stack)
            settle("first heartbeat") { stack.server.hits(HEARTBEAT) == 1 }
            assertTrue(stack.server.bodyOf(HEARTBEAT).contains(""""service_ok":false"""))

            proxy.serving.value = true
            settle("early heartbeat") { stack.server.hits(HEARTBEAT) == 2 }
            assertTrue(stack.server.bodiesOf(HEARTBEAT).last().contains(""""service_ok":true"""))
        }

    @Test
    fun `a vpn on the tv pauses sharing with its own reason`() = screen { stack ->
        stack.server.always(
            HEARTBEAT,
            body = """{"bound":true,"status":"paused","reason":"service_down","interval_id":null}""",
        )
        val engine = engine(stack)
        val state = observe(engine)
        start(engine, stack)
        settle("paused") { state()?.status == SharingStatus.Paused(PauseReason.OTHER) }

        proxy.stopReason.value = ProxyCommand.REASON_VPN_OR_PROXY

        settle("vpn") { state()?.status == SharingStatus.Paused(PauseReason.VPN_OR_PROXY) }
    }

    @Test
    fun `consent the backend asks for wins over the proxy's reason`() = screen { stack ->
        proxy.stopReason.value = ProxyCommand.REASON_VPN_OR_PROXY
        stack.server.always(
            HEARTBEAT,
            body = """{"bound":true,"status":"paused","reason":"consent_required","interval_id":null}""",
        )
        val engine = engine(stack)
        val state = observe(engine)

        start(engine, stack)

        settle("consent") { state()?.status == SharingStatus.Paused(PauseReason.CONSENT_REQUIRED) }
    }

    @Test
    fun `no proxy server for the tv pauses an otherwise active tv`() = screen { stack ->
        stack.server.always(HEARTBEAT, body = ACTIVE)
        val engine = engine(stack)
        val state = observe(engine)
        start(engine, stack)
        settle("active") { state()?.status == SharingStatus.Active }

        proxy.stopReason.value = ProxyCommand.REASON_NO_CAPACITY
        settle("no proxy") { state()?.status == SharingStatus.Paused(PauseReason.NO_PROXY) }

        proxy.stopReason.value = null
        settle("active again") { state()?.status == SharingStatus.Active }
    }

    private fun engine(stack: TestApiStack) = SharingEngine(
        heartbeats = HeartbeatRepository(stack.api, stack.serverClock),
        networkMonitor = network,
        permissions = permissions,
        sessionStore = stack.sessionStore,
        proxy = proxy,
    )

    private suspend fun TestScope.start(engine: SharingEngine, stack: TestApiStack): Job {
        stack.sessionStore.open(SessionTokens("tv.access", "tv.refresh", "binding"))
        return backgroundScope.launch { engine.run() }
    }

    private fun TestScope.observe(engine: SharingEngine): () -> SharingState? {
        var latest: SharingState? = null
        backgroundScope.launch { engine.state.collect { latest = it } }
        return { latest }
    }

    private companion object {
        const val HEARTBEAT = "/api/v1/iontv/tv/heartbeat"
        const val MINUTE = 60_000L
    }
}
