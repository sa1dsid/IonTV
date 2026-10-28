package com.ionica.iontv.feature.dashboard

import com.ionica.iontv.core.api.Paths
import com.ionica.iontv.core.api.Server
import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.core.network.Connection
import com.ionica.iontv.core.session.SessionTokens
import com.ionica.iontv.core.system.FakeBackgroundPermissions
import com.ionica.iontv.feature.consent.ConsentPaths
import com.ionica.iontv.feature.consent.assertConsentRecord
import com.ionica.iontv.feature.consent.data.ConsentRepository
import com.ionica.iontv.feature.consent.data.CONSENT_VERSION
import com.ionica.iontv.feature.dashboard.data.UnlinkRepository
import com.ionica.iontv.feature.dashboard.presentation.DashboardEvent
import com.ionica.iontv.feature.dashboard.presentation.DashboardViewModel
import com.ionica.iontv.feature.sharing.SharingMonitor
import com.ionica.iontv.feature.sharing.domain.model.PauseReason
import com.ionica.iontv.feature.sharing.domain.model.SharingState
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus
import com.ionica.iontv.feature.sharing.service.SharingController
import com.ionica.iontv.feature.update.config
import com.ionica.iontv.feature.update.data.AppUpdateRepository
import com.ionica.iontv.testing.ViewModelTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CONFIG = "/api/v1/iontv/config"

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest : ViewModelTestCase() {

    private val sharing = FakeSharingMonitor()

    private val controller = FakeSharingController()

    private val permissions = FakeBackgroundPermissions()

    @Test
    fun `the dashboard keeps sharing running and shows its status`() = screen { stack ->
        linked(stack)
        sharing.state.value = SharingState(SharingStatus.Active, Connection.ETHERNET)

        val viewModel = dashboard(stack)
        settle("status") { viewModel.uiState.value.connection == Connection.ETHERNET }

        assertEquals(1, controller.starts)
        assertEquals(SharingStatus.Active, viewModel.uiState.value.status)

        sharing.state.value = SharingState(SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION), Connection.WIFI)
        runCurrent()
        assertEquals(SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION), viewModel.uiState.value.status)

        viewModel.onOpenSettingsClick()
        viewModel.onResume()
        assertEquals(listOf(FakeBackgroundPermissions.BATTERY), permissions.opened)
        assertEquals(1, sharing.rechecks)
    }

    @Test
    fun `holding ok asks before unlinking and keep linked closes the question`() = screen { stack ->
        linked(stack)
        val viewModel = dashboard(stack)

        viewModel.onLongPressOk()
        assertTrue(viewModel.uiState.value.confirmingUnlink)

        viewModel.onKeepLinkedClick()
        assertFalse(viewModel.uiState.value.confirmingUnlink)
        assertEquals(0, controller.stops)
    }

    @Test
    fun `unlinking withdraws consent, revokes the tv session and forgets it`() = screen { stack ->
        linked(stack)
        val deviceId = stack.sessionStore.deviceId()
        val consentPath = ConsentPaths.of(deviceId)
        stack.server.next(consentPath, body = """{"granted":false}""")
        stack.server.next(Paths.LOGOUT, code = 204, body = "")
        val viewModel = dashboard(stack)
        val events = mutableListOf<DashboardEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onLongPressOk()
        viewModel.onUnlinkClick()
        settle("unlinked") { events.isNotEmpty() }

        assertEquals(listOf(DashboardEvent.Unlinked), events)
        assertEquals(1, controller.stops)
        assertConsentRecord(stack.server.bodyOf(consentPath), granted = false, screen = "dashboard")
        assertEquals(listOf("Bearer tv.access"), stack.server.authorizationsOf(consentPath))
        assertEquals(listOf("Bearer tv.access"), stack.server.authorizationsOf(Paths.LOGOUT))
        assertNull(stack.sessionStore.current())
        assertNotEquals(deviceId, stack.sessionStore.deviceId())
    }

    @Test
    fun `unlinking without a network still unlinks the tv`() = screen { stack ->
        linked(stack)
        stack.server.nextDropsConnection(ConsentPaths.of(stack.sessionStore.deviceId()))
        stack.server.nextDropsConnection(Paths.LOGOUT)
        val viewModel = dashboard(stack)
        val events = mutableListOf<DashboardEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onUnlinkClick()
        settle("unlinked") { events.isNotEmpty() }

        assertEquals(listOf(DashboardEvent.Unlinked), events)
        assertNull(stack.sessionStore.current())
    }

    @Test
    fun `a newer version puts up the update banner, and a failed check does not take it down`() = screen { stack ->
        linked(stack)
        stack.server.next(CONFIG, body = config(latest = "1.1.0"))
        val viewModel = dashboard(stack)

        viewModel.onResume()
        settle("banner") { viewModel.uiState.value.update != null }
        assertEquals("1.1.0", viewModel.uiState.value.update?.versionName)

        stack.server.next(CONFIG, code = 503, body = Server.detail("IONTV is disabled"))
        viewModel.onWake()
        settle("second check") { stack.server.hits(CONFIG) == 2 }
        assertEquals("1.1.0", viewModel.uiState.value.update?.versionName)
    }

    @Test
    fun `a backend without our consent sends the tv back to the consent screen`() = screen { stack ->
        linked(stack)
        stack.sessionStore.saveConsent(CONSENT_VERSION)
        val viewModel = dashboard(stack)
        val events = mutableListOf<DashboardEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        sharing.state.value = SharingState(SharingStatus.Paused(PauseReason.CONSENT_REQUIRED), Connection.WIFI)
        settle("consent required") { events.isNotEmpty() }

        assertEquals(listOf(DashboardEvent.ConsentRequired), events)
        assertNull(stack.sessionStore.consentVersion())
    }

    private suspend fun linked(stack: TestApiStack) {
        stack.sessionStore.open(SessionTokens("tv.access", "tv.refresh", "binding"))
    }

    private fun dashboard(stack: TestApiStack) = DashboardViewModel(
        sharing = sharing,
        sharingController = controller,
        sessionStore = stack.sessionStore,
        permissions = permissions,
        unlinkRepository = UnlinkRepository(
            api = stack.api,
            sessionStore = stack.sessionStore,
            consent = ConsentRepository(stack.api, stack.sessionStore, stack.serverClock),
            sharingController = controller,
        ),
        updates = AppUpdateRepository(stack.unauthenticatedApi, installedVersion = "1.0.0"),
    )
}

internal class FakeSharingMonitor : SharingMonitor {

    override val state = MutableStateFlow(SharingState(SharingStatus.Active, Connection.WIFI))

    var rechecks = 0
        private set

    override fun recheckBattery() {
        rechecks++
    }

    var refreshes = 0
        private set

    override fun refresh() {
        refreshes++
    }
}

internal class FakeSharingController : SharingController {

    var starts = 0
        private set

    var stops = 0
        private set

    override fun start() {
        starts++
    }

    override fun stop() {
        stops++
    }
}
