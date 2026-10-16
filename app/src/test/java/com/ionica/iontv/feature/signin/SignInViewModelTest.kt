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
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val QR_URL = "https://pulse.example/link#token=qr-token-0123456789-0123456789"

private const val PAIRING = """{"pair_id":"b12e0880-195f-4af6-93aa-93c7035195d0","code":"7k2m4q",
    "token":"qr-token-0123456789-0123456789","poll_token":"poll-token-0123456789-0123456789-abcdef",
    "expires_in":300,"poll_interval":2,"qr_url":"$QR_URL"}"""

private const val NEXT_PAIRING = """{"pair_id":"0f9a7c1e-3c55-4c2c-9d1e-6a1d2b3c4d5e","code":"MNDEAPXW",
    "token":"qr-token-9876543210-9876543210","poll_token":"poll-token-9876543210-9876543210-fedcba",
    "expires_in":300,"poll_interval":2,"qr_url":"https://pulse.example/link#token=next"}"""

private const val PENDING = """{"status":"pending"}"""

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest : ViewModelTestCase() {

    @Test
    fun `the tv draws the site link as a qr and shows the short code under it`() = screen { stack ->
        stack.server.next(PairingPaths.CODE, body = PAIRING)

        val viewModel = signIn(stack)
        settle("pairing") { viewModel.uiState.value.pairing is PairingUi.Ready }

        // Регистр в коде не важен, показываем заглавными — как в макете.
        assertEquals(PairingUi.Ready(code = "7K2M4Q", qrContent = QR_URL), viewModel.uiState.value.pairing)
        val deviceId = stack.sessionStore.deviceId()
        assertEquals(
            """{"device_id":"$deviceId","name":"Living room TV"}""",
            stack.server.bodyOf(PairingPaths.CODE),
        )
    }

    @Test
    fun `the tv asks every two seconds until the site confirms and then keeps the tv session`() =
        screen { stack ->
            stack.server.next(PairingPaths.CODE, body = PAIRING)
            stack.server.next(PairingPaths.POLL, body = PENDING)
            stack.server.next(PairingPaths.POLL, body = paired())
            val viewModel = signIn(stack)
            val events = mutableListOf<SignInEvent>()
            backgroundScope.launch { viewModel.events.toList(events) }

            settle("pairing") { viewModel.uiState.value.pairing is PairingUi.Ready }
            advanceTimeBy(2_001)
            settle("first poll") { stack.server.hits(PairingPaths.POLL) == 1 }
            assertTrue(events.isEmpty())

            advanceTimeBy(2_000)
            settle("linked") { events.isNotEmpty() }

            assertEquals(listOf(SignInEvent.Linked), events)
            assertEquals(
                """{"pair_id":"b12e0880-195f-4af6-93aa-93c7035195d0","poll_token":"poll-token-0123456789-0123456789-abcdef"}""",
                stack.server.bodyOf(PairingPaths.POLL),
            )
            assertEquals("tv.access.token", stack.sessionStore.current()?.accessToken)

            advanceTimeBy(30_000)
            assertEquals(2, stack.server.hits(PairingPaths.POLL))
        }

    @Test
    fun `an expired pairing is replaced with a new qr by itself`() = screen { stack ->
        stack.server.next(PairingPaths.CODE, body = PAIRING)
        stack.server.next(PairingPaths.POLL, code = 410, body = Server.detail("Pairing expired"))
        stack.server.next(PairingPaths.CODE, body = NEXT_PAIRING)
        stack.server.always(PairingPaths.POLL, body = PENDING)
        val viewModel = signIn(stack)

        settle("pairing") { viewModel.uiState.value.pairing is PairingUi.Ready }
        advanceTimeBy(2_001)

        settle("new pairing") { (viewModel.uiState.value.pairing as? PairingUi.Ready)?.code == "MNDEAPXW" }
        assertEquals(2, stack.server.hits(PairingPaths.CODE))
    }

    @Test
    fun `the qr is renewed after its lifetime even if the server stays silent`() = screen { stack ->
        stack.server.next(PairingPaths.CODE, body = PAIRING)
        stack.server.next(PairingPaths.CODE, body = NEXT_PAIRING)
        stack.server.always(PairingPaths.POLL, body = PENDING)
        val viewModel = signIn(stack)

        settle("pairing") { viewModel.uiState.value.pairing is PairingUi.Ready }
        advanceTimeBy(299_000)
        assertEquals("7K2M4Q", (viewModel.uiState.value.pairing as? PairingUi.Ready)?.code)

        advanceTimeBy(2_000)
        settle("new pairing") { (viewModel.uiState.value.pairing as? PairingUi.Ready)?.code == "MNDEAPXW" }
    }

    @Test
    fun `without a connection the tv says so and asks for a pairing again by itself`() = screen { stack ->
        stack.server.nextDropsConnection(PairingPaths.CODE)
        val viewModel = signIn(stack)

        settle("offline") { viewModel.uiState.value.pairing == PairingUi.Offline }

        stack.server.next(PairingPaths.CODE, body = PAIRING)
        advanceTimeBy(5_001)

        settle("pairing") { viewModel.uiState.value.pairing is PairingUi.Ready }
    }

    private fun signIn(stack: TestApiStack): SignInViewModel = SignInViewModel(
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
