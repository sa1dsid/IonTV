package com.ionica.iontv.feature.setup

import com.ionica.iontv.core.system.DeviceSetupStore
import com.ionica.iontv.core.system.FakeBackgroundPermissions
import com.ionica.iontv.feature.setup.presentation.SetupEvent
import com.ionica.iontv.feature.setup.presentation.SetupStep
import com.ionica.iontv.feature.setup.presentation.SetupViewModel
import com.ionica.iontv.testing.ViewModelTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest : ViewModelTestCase() {

    private val permissions = FakeBackgroundPermissions()

    private val setupStore by lazy { DeviceSetupStore(preferences("device")) }

    @Test
    fun `battery optimization switched off earlier is ticked right away`() = screen {
        permissions.batteryIgnored = true
        val viewModel = setup()

        viewModel.onResume()
        settle("ticked") { viewModel.uiState.value.backgroundActivityDone }

        assertFalse(viewModel.uiState.value.autostartDone)
        assertFalse(viewModel.uiState.value.canContinue)
        assertTrue(permissions.opened.isEmpty())
    }

    @Test
    fun `background activity is ticked once battery optimization is really off`() = screen {
        val viewModel = setup()
        viewModel.onResume()

        viewModel.onOpenSettingsClick(SetupStep.BACKGROUND_ACTIVITY)
        assertEquals(listOf(FakeBackgroundPermissions.BATTERY), permissions.opened)

        // Вернулся, ничего не поменяв, — галки нет.
        viewModel.onResume()
        settle("visit saved") { setupStoreRead { batterySettingsVisited() } }
        assertFalse(viewModel.uiState.value.backgroundActivityDone)

        permissions.batteryIgnored = true
        viewModel.onOpenSettingsClick(SetupStep.BACKGROUND_ACTIVITY)
        viewModel.onResume()
        settle("ticked") { viewModel.uiState.value.backgroundActivityDone }
    }

    @Test
    fun `a tv with no screen for battery exemption counts the visit`() = screen {
        permissions.exemptionScreen = false
        val viewModel = setup()

        viewModel.onOpenSettingsClick(SetupStep.BACKGROUND_ACTIVITY)
        viewModel.onResume()

        settle("ticked") { viewModel.uiState.value.backgroundActivityDone }
    }

    @Test
    fun `autostart is ticked after a trip to the settings and is remembered`() = screen {
        val viewModel = setup()

        viewModel.onOpenSettingsClick(SetupStep.AUTOSTART)
        viewModel.onResume()
        settle("ticked") { viewModel.uiState.value.autostartDone }

        val restarted = setup()
        restarted.onResume()
        settle("remembered") { restarted.uiState.value.autostartDone }
    }

    @Test
    fun `continue finishes the setup only when both steps are done`() = screen {
        val viewModel = setup()
        val events = mutableListOf<SetupEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }

        viewModel.onContinueClick()
        assertTrue(events.isEmpty())

        permissions.batteryIgnored = true
        viewModel.onOpenSettingsClick(SetupStep.AUTOSTART)
        viewModel.onResume()
        settle("both ticked") { viewModel.uiState.value.canContinue }

        viewModel.onContinueClick()
        settle("finished") { events.isNotEmpty() }

        assertEquals(listOf(SetupEvent.Finished), events)
        assertTrue(setupStoreRead { isSetupCompleted() })
    }

    private fun setup() = SetupViewModel(permissions = permissions, setupStore = setupStore)

    private fun setupStoreRead(read: suspend DeviceSetupStore.() -> Boolean): Boolean =
        runBlocking { setupStore.read() }
}
