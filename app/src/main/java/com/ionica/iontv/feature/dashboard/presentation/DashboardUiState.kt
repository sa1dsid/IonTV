package com.ionica.iontv.feature.dashboard.presentation

import com.ionica.iontv.core.network.Connection
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus
import com.ionica.iontv.feature.update.domain.model.AppUpdate

data class DashboardUiState(
    val status: SharingStatus = SharingStatus.Active,
    val connection: Connection = Connection.NONE,
    val update: AppUpdate? = null,
    val confirmingUnlink: Boolean = false,
    val unlinking: Boolean = false,
)

sealed interface DashboardEvent {
    data object Unlinked : DashboardEvent

    data object ConsentRequired : DashboardEvent
}
