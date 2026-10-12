package com.ionica.iontv.feature.signin

import com.ionica.iontv.core.api.Server
import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.core.device.DeviceName
import com.ionica.iontv.feature.signin.data.PairingRepository
import com.ionica.iontv.feature.signin.presentation.PairingUi
import com.ionica.iontv.feature.signin.presentation.SignInEvent
import com.ionica.iontv.feature.signin.presentation.SignInViewModel
import com.ionica.iontv.testing.ViewModelTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CODE = """{"code":"7K2M4Q","poll_token":"poll-token-0123456789-0123456789-abcdef","expires_in":300}"""

private const val NEXT_CODE = """{"code":"MNDEAP","poll_token":"poll-token-9876543210-9876543210-fedcba","expires_in":300}"""

private const val PENDING = """{"status":"pending"}"""

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest : ViewModelTestCase() {

    @Test
    fun `the tv shows its pairing code and introduces itself by name`() = screen { stack ->
        stack.server.next(PairingPaths.CODE, body = CODE)

        val viewModel = signIn(stack)
        settle("code") { viewModel.uiState.value.pairing is PairingUi.Ready }

        assertEquals(PairingUi.Ready("7K2M4Q"), viewModel.uiState.value.pairing)
        val deviceId = stack.sessionStore.deviceId()
        assertEquals(
            """{"device_id":"$deviceId","name":"Living room TV"}""",
            stack.server.bodyOf(PairingPaths.CODE),
        )
    }

    @Test
    fun `the tv polls until the code is entered and then keeps the tv session`() = screen { stack ->
        stack.server.next(PairingPaths.CODE, body = CODE)
        stack.server.next(PairingPaths.POLL, body = PENDING)
        stack.server.next(PairingPaths.POLL, body = paired())
        val viewModel = signIn(stack)
        val events = mutableListOf<SignInEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        settle("code") { viewModel.uiState.value.pairing is PairingUi.Ready }
        advanceTimeBy(3_001)
        assertTrue(events.isEmpty())

        advanceTimeBy(3_000)
        settle("linked") { events.isNotEmpty() }

        assertEquals(listOf(SignInEvent.Linked), events)
        assertEquals(
            """{"code":"7K2M4Q","poll_token":"poll-token-0123456789-0123456789-abcdef"}""",
            stack.server.bodyOf(PairingPaths.POLL),
        )
        val session = stack.sessionStore.current()
        assertEquals("tv.access.token", session?.accessToken)
        assertNull(session?.username)

        advanceTimeBy(30_000)
        assertEquals(2, stack.server.hits(PairingPaths.POLL))
    }

    @Test
    fun `a code the server forgot turns into code expired and refresh brings a new one`() =
        screen { stack ->
            stack.server.next(PairingPaths.CODE, body = CODE)
            stack.server.next(PairingPaths.POLL, code = 404, body = Server.detail("Pairing code not found"))
            stack.server.next(PairingPaths.CODE, body = NEXT_CODE)
            val viewModel = signIn(stack)

            settle("code") { viewModel.uiState.value.pairing is PairingUi.Ready }
            advanceTimeBy(3_001)
            settle("expired") { viewModel.uiState.value.pairing == PairingUi.Expired }

            viewModel.onRefreshClick()
            settle("new code") { viewModel.uiState.value.pairing == PairingUi.Ready("MNDEAP") }
            assertEquals(2, stack.server.hits(PairingPaths.CODE))
        }

    @Test
    fun `the code expires on the tv after its lifetime even if the server stays silent`() =
        screen { stack ->
            stack.server.next(PairingPaths.CODE, body = CODE)
            stack.server.always(PairingPaths.POLL, body = PENDING)
            val viewModel = signIn(stack)

            settle("code") { viewModel.uiState.value.pairing is PairingUi.Ready }
            advanceTimeBy(299_000)
            assertEquals(PairingUi.Ready("7K2M4Q"), viewModel.uiState.value.pairing)

            advanceTimeBy(2_000)
            assertEquals(PairingUi.Expired, viewModel.uiState.value.pairing)
        }

    @Test
    fun `without a connection the tv says so and asks for a code again by itself`() = screen { stack ->
        stack.server.nextDropsConnection(PairingPaths.CODE)
        val viewModel = signIn(stack)

        settle("offline") { viewModel.uiState.value.pairing == PairingUi.Offline }

        stack.server.next(PairingPaths.CODE, body = CODE)
        advanceTimeBy(5_001)

        settle("code") { viewModel.uiState.value.pairing == PairingUi.Ready("7K2M4Q") }
    }

    private fun TestScope.signIn(stack: TestApiStack) = SignInViewModel(
        pairing = PairingRepository(stack.api, stack.sessionStore, DeviceName { "Living room TV" }),
    )

    private fun paired(): String = """
        {"access_token":"tv.access.token","refresh_token":"tv.refresh.token","token_type":"bearer",
        "expires_in":900,"binding_id":"4a0c38c8-6a5e-4b67-9a0e-1b5c7f2e9d11","status":"paired"}
    """
}

internal object PairingPaths {
    const val CODE = "/api/v1/iontv/pairing/code"
    const val POLL = "/api/v1/iontv/pairing/poll"
}
