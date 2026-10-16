package com.ionica.iontv.feature.dashboard

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.core.network.Connection
import com.ionica.iontv.feature.dashboard.presentation.AMBIENT_TAG
import com.ionica.iontv.feature.dashboard.presentation.DashboardScreen
import com.ionica.iontv.feature.dashboard.presentation.DashboardUiState
import com.ionica.iontv.feature.sharing.domain.model.PauseReason
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TV_1080P = "w960dp-h540dp-xlarge-land-television-xhdpi-keyshidden-nonav"

private const val UNLINK_TITLE = "Unlink this TV?"

private const val OPEN_SETTINGS = "Open settings"

/** Удержание OK как на настоящем пульте: нажатие, повторы с 400 мс каждые 50 мс, отпускание. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TV_1080P)
class RemoteHoldTest {

    @get:Rule
    val compose = createComposeRule()

    private var keepLinkedClicks = 0
    private var unlinkClicks = 0
    private var settingsClicks = 0

    @Test
    fun `releasing the held OK leaves the unlink question on screen`() {
        dashboard()

        holdOk(millis = 1_300)

        compose.onNodeWithText(UNLINK_TITLE).assertExists()
        assertEquals(0, keepLinkedClicks)
        assertEquals(0, unlinkClicks)
    }

    @Test
    fun `the next press answers the unlink question`() {
        dashboard()
        holdOk(millis = 1_300)

        compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        settle()

        assertEquals(1, keepLinkedClicks)
        compose.onNodeWithText(UNLINK_TITLE).assertDoesNotExist()
    }

    @Test
    fun `back on the remote keeps the tv linked`() {
        dashboard()
        holdOk(millis = 1_300)

        compose.onRoot().performKeyInput { pressKey(Key.Back) }
        settle()

        assertEquals(1, keepLinkedClicks)
        assertEquals(0, unlinkClicks)
        compose.onNodeWithText(UNLINK_TITLE).assertDoesNotExist()
    }

    @Test
    fun `keep linked on a paused dashboard gives the focus back to open settings`() {
        dashboard(DashboardUiState(status = SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION)))
        // Телевизор без сенсора сразу отдаёт кнопке фокус; Robolectric стартует в режиме касаний.
        compose.onNodeWithText(OPEN_SETTINGS).performSemanticsAction(SemanticsActions.RequestFocus)
        settle()
        holdOk(millis = 1_300)
        compose.onNodeWithText(UNLINK_TITLE).assertExists()

        compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        settle()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        settle()

        assertEquals(1, keepLinkedClicks)
        assertEquals(1, settingsClicks)
    }

    @Test
    fun `while unlinking the popup ignores the remote`() {
        dashboard(DashboardUiState(confirmingUnlink = true, unlinking = true))

        compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onRoot().performKeyInput { pressKey(Key.Back) }
        settle()

        assertEquals(0, keepLinkedClicks)
        assertEquals(0, unlinkClicks)
        compose.onNodeWithText(UNLINK_TITLE).assertExists()
    }

    @Test
    fun `a short hold opens nothing`() {
        dashboard()

        holdOk(millis = 600)

        compose.onNodeWithText(UNLINK_TITLE).assertDoesNotExist()
    }

    @Test
    fun `waking a paused dashboard does not press open settings`() {
        dashboard(DashboardUiState(status = SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION)))
        compose.mainClock.advanceTimeBy(61_000)
        compose.onNodeWithTag(AMBIENT_TAG).assertExists()

        holdOk(millis = 150)

        compose.onNodeWithTag(AMBIENT_TAG).assertDoesNotExist()
        assertEquals(0, settingsClicks)
    }

    private fun holdOk(millis: Long) {
        val down = 1_000L
        send(down, down, ACTION_DOWN, repeat = 0)
        var repeat = 0
        var at = down + 400
        while (at < down + millis) {
            send(down, at, ACTION_DOWN, repeat = ++repeat)
            at += 50
        }
        send(down, down + millis, ACTION_UP, repeat = 0)
    }

    // Между событиями пульта проходит кадр: попап успевает появиться и забрать фокус.
    private fun send(downTime: Long, eventTime: Long, action: Int, repeat: Int) {
        val native = android.view.KeyEvent(downTime, eventTime, action, KEYCODE_DPAD_CENTER, repeat)
        compose.onRoot().performKeyPress(KeyEvent(native))
        settle()
    }

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun dashboard(
        initial: DashboardUiState = DashboardUiState(connection = Connection.ETHERNET),
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            var uiState by remember { mutableStateOf(initial) }
            IonTvTheme {
                DashboardScreen(
                    uiState = uiState,
                    onOpenSettingsClick = { settingsClicks++ },
                    onLongPressOk = { uiState = uiState.copy(confirmingUnlink = true) },
                    onKeepLinkedClick = {
                        keepLinkedClicks++
                        uiState = uiState.copy(confirmingUnlink = false)
                    },
                    onUnlinkClick = { unlinkClicks++ },
                )
            }
        }
        compose.mainClock.advanceTimeBy(100)
    }
}
