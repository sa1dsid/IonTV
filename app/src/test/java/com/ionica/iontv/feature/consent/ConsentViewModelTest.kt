package com.ionica.iontv.feature.consent

import com.ionica.iontv.core.api.Server
import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.core.session.SessionTokens
import com.ionica.iontv.core.system.DeviceSetupStore
import com.ionica.iontv.feature.consent.data.CONSENT_VERSION
import com.ionica.iontv.feature.consent.data.ConsentRepository
import com.ionica.iontv.feature.consent.presentation.ConsentError
import com.ionica.iontv.feature.consent.presentation.ConsentEvent
import com.ionica.iontv.feature.consent.presentation.ConsentViewModel
import com.ionica.iontv.testing.ViewModelTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentViewModelTest : ViewModelTestCase() {

    private val setupStore by lazy { DeviceSetupStore(preferences("device")) }

    @Test
    fun `agreeing records the consent for this tv with the text version`() = screen { stack ->
        linked(stack)
        val deviceId = stack.sessionStore.deviceId()
        val path = ConsentPaths.of(deviceId)
        stack.server.next(path, body = """{"granted":true}""")
        val viewModel = consent(stack)
        val events = mutableListOf<ConsentEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onAgreeClick()
        settle("agreed") { events.isNotEmpty() }

        assertEquals(listOf(ConsentEvent.Agreed(setupPending = true)), events)
        assertEquals(
            """{"granted":true,"version":"$CONSENT_VERSION","screen":"consent"}""",
            stack.server.bodyOf(path),
        )
        assertEquals(listOf("Bearer tv.access"), stack.server.authorizationsOf(path))
        assertEquals(CONSENT_VERSION, stack.sessionStore.consentVersion())
    }

    @Test
    fun `a tv that already went through the checklist skips it after consent`() = screen { stack ->
        linked(stack)
        setupStore.markSetupCompleted()
        stack.server.next(ConsentPaths.of(stack.sessionStore.deviceId()), body = """{"granted":true}""")
        val viewModel = consent(stack)
        val events = mutableListOf<ConsentEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onAgreeClick()
        settle("agreed") { events.isNotEmpty() }

        assertEquals(listOf(ConsentEvent.Agreed(setupPending = false)), events)
    }

    @Test
    fun `a failed request keeps the tv on the consent screen with a line`() = screen { stack ->
        linked(stack)
        stack.server.next(ConsentPaths.of(stack.sessionStore.deviceId()), code = 503, body = Server.detail("IONTV is disabled"))
        val viewModel = consent(stack)
        val events = mutableListOf<ConsentEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onAgreeClick()
        settle("error") { viewModel.uiState.value.error != null }

        assertEquals(ConsentError.SERVER, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.submitting)
        assertTrue(events.isEmpty())
        assertNull(stack.sessionStore.consentVersion())
    }

    @Test
    fun `declining shows the popup and only go back or exit leave it`() = screen { stack ->
        linked(stack)
        val viewModel = consent(stack)
        val events = mutableListOf<ConsentEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onDeclineClick()
        assertTrue(viewModel.uiState.value.declining)

        viewModel.onGoBackClick()
        assertFalse(viewModel.uiState.value.declining)

        viewModel.onDeclineClick()
        viewModel.onExitClick()
        runCurrent()
        assertEquals(listOf(ConsentEvent.ExitApp), events)
    }

    private suspend fun linked(stack: TestApiStack) {
        stack.sessionStore.open(SessionTokens("tv.access", "tv.refresh", "binding"), username = null)
    }

    private fun consent(stack: TestApiStack) = ConsentViewModel(
        consent = ConsentRepository(stack.api, stack.sessionStore),
        setupStore = setupStore,
    )
}

internal object ConsentPaths {
    fun of(deviceId: String) = "/api/v1/iontv/device/$deviceId/consent"
}
