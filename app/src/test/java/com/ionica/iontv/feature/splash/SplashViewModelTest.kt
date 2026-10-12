package com.ionica.iontv.feature.splash

import com.ionica.iontv.core.api.BackendHealth
import com.ionica.iontv.core.api.Paths
import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.core.common.TimeSource
import com.ionica.iontv.core.session.SessionTokens
import com.ionica.iontv.feature.splash.domain.ResolveStartDestinationUseCase
import com.ionica.iontv.feature.splash.domain.StartDestination
import com.ionica.iontv.feature.splash.presentation.SplashViewModel
import com.ionica.iontv.testing.ViewModelTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

private const val HEALTHY = """{"status":"ok","enabled":true}"""

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest : ViewModelTestCase() {

    @Test
    fun `a tv without a session goes to sign in`() = screen { stack ->
        stack.server.always(Paths.HEALTH, body = HEALTHY)

        val viewModel = splash(stack)
        advanceUntilIdle()

        assertEquals(StartDestination.SIGN_IN, viewModel.uiState.value.destination)
        assertFalse(viewModel.uiState.value.offline)
    }

    @Test
    fun `a linked tv goes straight to the dashboard`() = screen { stack ->
        stack.sessionStore.open(SessionTokens("access", "refresh", "binding"), username = null)
        stack.server.always(Paths.HEALTH, body = HEALTHY)

        val viewModel = splash(stack)
        advanceUntilIdle()

        assertEquals(StartDestination.DASHBOARD, viewModel.uiState.value.destination)
    }

    @Test
    fun `the splash stays a moment even when the check is instant`() = screen { stack ->
        stack.server.always(Paths.HEALTH, body = HEALTHY)

        val viewModel = splash(stack)
        runCurrent()
        assertNull(viewModel.uiState.value.destination)

        advanceTimeBy(700)
        assertEquals(StartDestination.SIGN_IN, viewModel.uiState.value.destination)
    }

    @Test
    fun `without a connection the splash says so and keeps retrying on its own`() = screen { stack ->
        stack.server.nextDropsConnection(Paths.HEALTH)
        stack.server.next(Paths.HEALTH, code = 503, body = HEALTHY)

        val viewModel = splash(stack)
        settle("offline line") { viewModel.uiState.value.offline }
        assertNull(viewModel.uiState.value.destination)

        stack.server.always(Paths.HEALTH, body = HEALTHY)
        settleIdle("destination") { viewModel.uiState.value.destination != null }

        assertEquals(3, stack.server.hits(Paths.HEALTH))
        assertFalse(viewModel.uiState.value.offline)
        assertEquals(StartDestination.SIGN_IN, viewModel.uiState.value.destination)
    }

    @Test
    fun `a switched off iontv counts as no connection`() = screen { stack ->
        stack.server.next(Paths.HEALTH, body = """{"status":"ok","enabled":false}""")
        stack.server.always(Paths.HEALTH, body = HEALTHY)

        val viewModel = splash(stack)
        settle("offline line") { viewModel.uiState.value.offline }

        settleIdle("destination") { viewModel.uiState.value.destination != null }
        assertEquals(StartDestination.SIGN_IN, viewModel.uiState.value.destination)
    }

    private fun TestScope.splash(stack: TestApiStack) = SplashViewModel(
        backendHealth = BackendHealth(stack.api),
        resolveStartDestination = ResolveStartDestinationUseCase(stack.sessionStore),
        timeSource = TimeSource { testScheduler.currentTime },
    )
}
