package com.ionica.iontv.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.core.system.BackgroundPermissions
import com.ionica.iontv.feature.dashboard.data.UnlinkRepository
import com.ionica.iontv.feature.sharing.SharingMonitor
import com.ionica.iontv.feature.sharing.domain.model.PauseReason
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus
import com.ionica.iontv.feature.sharing.service.SharingController
import com.ionica.iontv.feature.update.data.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sharing: SharingMonitor,
    private val sharingController: SharingController,
    private val sessionStore: SessionStore,
    private val permissions: BackgroundPermissions,
    private val unlinkRepository: UnlinkRepository,
    private val updates: AppUpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<DashboardEvent>(Channel.BUFFERED)
    val events: Flow<DashboardEvent> = eventChannel.receiveAsFlow()

    private var updateCheck: Job? = null

    init {
        sharingController.start()

        viewModelScope.launch {
            sharing.state.collect { state ->
                _uiState.update {
                    it.copy(status = state.status, connection = state.connection, username = state.username)
                }
                if (state.status == SharingStatus.Paused(PauseReason.CONSENT_REQUIRED)) {
                    sessionStore.saveConsent(null)
                    eventChannel.send(DashboardEvent.ConsentRequired)
                }
            }
        }
    }

    fun onResume() {
        sharing.recheckBattery()
        checkForUpdate()
    }

    fun onWake() {
        checkForUpdate()
    }

    fun onOpenSettingsClick() {
        permissions.openBatteryOptimizationSettings()
    }

    private fun checkForUpdate() {
        if (updateCheck?.isActive == true) return
        updateCheck = viewModelScope.launch {
            updates.check().onSuccess { update -> _uiState.update { it.copy(update = update) } }
        }
    }

    fun onLongPressOk() {
        if (_uiState.value.unlinking) return
        _uiState.update { it.copy(confirmingUnlink = true) }
    }

    fun onKeepLinkedClick() {
        if (_uiState.value.unlinking) return
        _uiState.update { it.copy(confirmingUnlink = false) }
    }

    fun onUnlinkClick() {
        if (_uiState.value.unlinking) return

        viewModelScope.launch {
            _uiState.update { it.copy(unlinking = true) }
            unlinkRepository.unlink()
            eventChannel.send(DashboardEvent.Unlinked)
        }
    }
}
