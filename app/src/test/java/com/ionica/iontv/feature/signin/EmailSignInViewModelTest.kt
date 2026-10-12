package com.ionica.iontv.feature.signin

import com.ionica.iontv.core.api.Paths
import com.ionica.iontv.core.api.Server
import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.core.device.DeviceName
import com.ionica.iontv.feature.signin.data.AccountLinkRepository
import com.ionica.iontv.feature.signin.presentation.email.EmailError
import com.ionica.iontv.feature.signin.presentation.email.EmailSignInEvent
import com.ionica.iontv.feature.signin.presentation.email.EmailSignInViewModel
import com.ionica.iontv.feature.signin.presentation.email.EmailStep
import com.ionica.iontv.testing.ViewModelTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EMAIL = "syrex@example.com"

private const val CODE_SENT = """{"resendAfterSeconds":60}"""

@OptIn(ExperimentalCoroutinesApi::class)
class EmailSignInViewModelTest : ViewModelTestCase() {

    @Test
    fun `a mistyped address is caught on the tv before any request`() = screen { stack ->
        val viewModel = emailSignIn(stack)

        viewModel.onEmailChange("syrex@")
        viewModel.onSendCodeClick()

        assertEquals(EmailError.EMAIL_INVALID, viewModel.uiState.value.error)
        assertEquals(0, stack.server.hits(EmailPaths.CODE))
    }

    @Test
    fun `sending the code moves to the code step and holds resend for a minute`() = screen { stack ->
        stack.server.next(EmailPaths.CODE, body = CODE_SENT)
        val viewModel = emailSignIn(stack)

        viewModel.onEmailChange(" $EMAIL ")
        viewModel.onSendCodeClick()
        runCurrent()

        assertEquals(EmailStep.CODE, viewModel.uiState.value.step)
        assertEquals(60, viewModel.uiState.value.resendInSeconds)
        assertEquals("""{"email":"$EMAIL"}""", stack.server.bodyOf(EmailPaths.CODE))

        advanceTimeBy(30_001)
        assertEquals(30, viewModel.uiState.value.resendInSeconds)
        viewModel.onResendClick()
        assertEquals(1, stack.server.hits(EmailPaths.CODE))

        advanceTimeBy(30_000)
        assertEquals(0, viewModel.uiState.value.resendInSeconds)
        stack.server.next(EmailPaths.CODE, body = CODE_SENT)
        viewModel.onResendClick()
        runCurrent()
        assertEquals(2, stack.server.hits(EmailPaths.CODE))
    }

    @Test
    fun `asking for a code too often says to wait`() = screen { stack ->
        stack.server.next(
            EmailPaths.CODE,
            code = 429,
            body = Server.error("RATE_LIMITED", "Wait 60 seconds before requesting a code"),
        )
        val viewModel = emailSignIn(stack)

        viewModel.onEmailChange(EMAIL)
        viewModel.onSendCodeClick()
        settle("error") { viewModel.uiState.value.error != null }

        assertEquals(EmailError.RESEND_TOO_SOON, viewModel.uiState.value.error)
        assertEquals(EmailStep.EMAIL, viewModel.uiState.value.step)
    }

    @Test
    fun `a wrong code marks the code field and keeps the tv unlinked`() = screen { stack ->
        val viewModel = onCodeStep(stack)
        stack.server.next(
            EmailPaths.LOGIN,
            code = 400,
            body = Server.error("INVALID_CODE", "Invalid sign-in code"),
        )

        viewModel.onCodeChange("407131")
        viewModel.onSignInClick()
        settle("error") { viewModel.uiState.value.error != null }

        assertEquals(EmailError.CODE_INVALID, viewModel.uiState.value.error)
        assertEquals(0, stack.server.hits(AccountPaths.REGISTER))
        assertNull(stack.sessionStore.current())
    }

    @Test
    fun `the right code links the tv and throws the account session away`() = screen { stack ->
        val viewModel = onCodeStep(stack)
        val events = mutableListOf<EmailSignInEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }
        stack.server.next(EmailPaths.LOGIN, body = accountLogin())
        stack.server.next(AccountPaths.REGISTER, body = Server.tokens(access = "tv.access", refresh = "tv.refresh"))
        stack.server.next(Paths.LOGOUT, code = 204, body = "")

        viewModel.onCodeChange("40 71 31")
        viewModel.onSignInClick()
        settle("linked") { events.isNotEmpty() }

        assertEquals(listOf(EmailSignInEvent.Linked), events)
        assertEquals(
            """{"email":"$EMAIL","code":"407131","clientType":"native"}""",
            stack.server.bodyOf(EmailPaths.LOGIN),
        )
        assertEquals(listOf("Bearer account.access"), stack.server.authorizationsOf(AccountPaths.REGISTER))
        assertEquals(listOf("Bearer account.access"), stack.server.authorizationsOf(Paths.LOGOUT))
        assertEquals("syrex", stack.sessionStore.current()?.username)
    }

    @Test
    fun `back from the code step returns to the address and back again leaves`() = screen { stack ->
        val viewModel = onCodeStep(stack)
        val events = mutableListOf<EmailSignInEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onBack()
        runCurrent()
        assertEquals(EmailStep.EMAIL, viewModel.uiState.value.step)
        assertEquals(EMAIL, viewModel.uiState.value.email)
        assertTrue(events.isEmpty())

        viewModel.onBack()
        runCurrent()
        assertEquals(listOf(EmailSignInEvent.Back), events)
    }

    private fun TestScope.onCodeStep(stack: TestApiStack): EmailSignInViewModel {
        stack.server.next(EmailPaths.CODE, body = CODE_SENT)
        return emailSignIn(stack).apply {
            onEmailChange(EMAIL)
            onSendCodeClick()
            runCurrent()
        }
    }

    private fun emailSignIn(stack: TestApiStack) = EmailSignInViewModel(
        accountLink = AccountLinkRepository(stack.api, stack.sessionStore, DeviceName { "Living room TV" }),
    )

    private fun accountLogin(): String = """
        {"accessToken":"account.access","expiresIn":900,"isNewUser":true,
        "user":{"id":"39","username":"syrex","email":"$EMAIL"},"refreshToken":"account.refresh"}
    """
}

internal object EmailPaths {
    const val CODE = "/api/v1/iontv/auth/email/code"
    const val LOGIN = "/api/v1/iontv/auth/email/login"
}
