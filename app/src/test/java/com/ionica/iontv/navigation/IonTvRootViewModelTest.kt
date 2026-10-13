package com.ionica.iontv.navigation

import com.ionica.iontv.core.session.SessionTokens
import com.ionica.iontv.testing.ViewModelTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IonTvRootViewModelTest : ViewModelTestCase() {

    @Test
    fun `a tv that starts unlinked hears nothing until a session it had ends`() = screen { stack ->
        val ends = mutableListOf<Unit>()
        backgroundScope.launch { IonTvRootViewModel(stack.sessionStore).sessionEnded.toList(ends) }
        runCurrent()
        assertTrue(ends.isEmpty())

        stack.sessionStore.open(SessionTokens("access", "refresh", "binding"), username = null)
        settle("linked") { stack.sessionStore.blockingCurrent() != null }
        assertTrue(ends.isEmpty())

        stack.sessionStore.close()
        settle("session end") { ends.isNotEmpty() }
        assertEquals(1, ends.size)
    }

    @Test
    fun `a linked tv learns when its session is closed from the background`() = screen { stack ->
        stack.sessionStore.open(SessionTokens("access", "refresh", "binding"), username = null)
        val ends = mutableListOf<Unit>()
        backgroundScope.launch { IonTvRootViewModel(stack.sessionStore).sessionEnded.toList(ends) }
        runCurrent()

        stack.sessionStore.close()
        settle("session end") { ends.isNotEmpty() }
    }
}
