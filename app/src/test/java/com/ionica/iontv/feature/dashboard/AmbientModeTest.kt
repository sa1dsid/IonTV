package com.ionica.iontv.feature.dashboard

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.core.network.Connection
import com.ionica.iontv.feature.dashboard.presentation.AMBIENT_TAG
import com.ionica.iontv.feature.dashboard.presentation.DASHBOARD_TAG
import com.ionica.iontv.feature.dashboard.presentation.DashboardScreen
import com.ionica.iontv.feature.dashboard.presentation.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TV_1080P = "w960dp-h540dp-xlarge-land-television-xhdpi-keyshidden-nonav"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TV_1080P)
class AmbientModeTest {

    @get:Rule
    val compose = createComposeRule()

    private var longPresses = 0

    @Test
    fun `a minute without the remote dims the dashboard to one status line`() {
        dashboard()

        compose.mainClock.advanceTimeBy(59_000)
        compose.onNodeWithTag(AMBIENT_TAG).assertDoesNotExist()

        compose.mainClock.advanceTimeBy(2_000)
        compose.onNodeWithTag(AMBIENT_TAG).assertExists()
    }

    @Test
    fun `any remote button wakes the dashboard and goes no further`() {
        dashboard()
        compose.mainClock.advanceTimeBy(61_000)
        compose.onNodeWithTag(AMBIENT_TAG).assertExists()

        compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        settleInput()

        compose.onNodeWithTag(AMBIENT_TAG).assertDoesNotExist()
        assertEquals(0, longPresses)
    }

    @Test
    fun `pressing the remote starts the minute again`() {
        dashboard()
        compose.onNodeWithTag(DASHBOARD_TAG).assertIsFocused()
        compose.mainClock.advanceTimeBy(50_000)

        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        settleInput()
        compose.mainClock.advanceTimeBy(50_000)
        compose.onNodeWithTag(AMBIENT_TAG).assertDoesNotExist()

        compose.mainClock.advanceTimeBy(11_000)
        compose.onNodeWithTag(AMBIENT_TAG).assertExists()
    }

    @Test
    fun `the unlink question never goes dark`() {
        dashboard(DashboardUiState(confirmingUnlink = true))

        compose.mainClock.advanceTimeBy(120_000)

        compose.onNodeWithTag(AMBIENT_TAG).assertDoesNotExist()
    }

    private fun settleInput() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
    }

    private fun dashboard(
        uiState: DashboardUiState = DashboardUiState(connection = Connection.ETHERNET),
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            IonTvTheme {
                DashboardScreen(
                    uiState = uiState,
                    onOpenSettingsClick = {},
                    onLongPressOk = { longPresses++ },
                    onKeepLinkedClick = {},
                    onUnlinkClick = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(100)
    }
}
