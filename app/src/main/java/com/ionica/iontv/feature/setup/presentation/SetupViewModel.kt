package com.ionica.iontv.feature.setup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionica.iontv.core.system.BackgroundPermissions
import com.ionica.iontv.core.system.DeviceSetupStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Чеклист 4a–4c. Галки пользователь не ставит: кнопка ведёт в системные настройки,
 * а по возвращении приложение само спрашивает систему и зажигает галку.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val permissions: BackgroundPermissions,
    private val setupStore: DeviceSetupStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<SetupEvent>(Channel.BUFFERED)
    val events: Flow<SetupEvent> = eventChannel.receiveAsFlow()

    private var openedStep: SetupStep? = null

    fun onOpenSettingsClick(step: SetupStep) {
        val opened = when (step) {
            SetupStep.BACKGROUND_ACTIVITY -> permissions.openBatteryOptimizationSettings()
            SetupStep.AUTOSTART -> permissions.openAutostartSettings()
        }
        if (opened) openedStep = step
    }

    /** Экран снова на виду: при первом показе или после возврата из системных настроек. */
    fun onResume() {
        val returnedFrom = openedStep
        openedStep = null

        viewModelScope.launch {
            when (returnedFrom) {
                SetupStep.BACKGROUND_ACTIVITY -> setupStore.markBatterySettingsVisited()
                SetupStep.AUTOSTART -> setupStore.markAutostartVisited()
                null -> Unit
            }
            refresh()
        }
    }

    fun onContinueClick() {
        if (!_uiState.value.canContinue) return

        viewModelScope.launch {
            setupStore.markSetupCompleted()
            eventChannel.send(SetupEvent.Finished)
        }
    }

    private suspend fun refresh() {
        // Многие телевизоры не дают снять оптимизацию батареи вовсе — экрана для этого нет.
        // Тогда, как с автозапуском, хватает того, что пользователь сходил в настройки.
        val backgroundActivity = permissions.isBatteryOptimizationIgnored() ||
            (setupStore.batterySettingsVisited() && !permissions.canRequestBatteryExemption())

        // Системного статуса автозапуска у Android нет: сходил в настройки и вернулся — готово.
        val autostart = setupStore.autostartVisited()

        _uiState.update {
            it.copy(backgroundActivityDone = backgroundActivity, autostartDone = autostart)
        }
    }
}
