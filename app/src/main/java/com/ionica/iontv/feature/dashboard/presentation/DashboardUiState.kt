package com.ionica.iontv.feature.dashboard.presentation

import com.ionica.iontv.core.network.Connection
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus

data class DashboardUiState(
    val status: SharingStatus = SharingStatus.Active,
    val connection: Connection = Connection.NONE,
    /** Попап 3c после долгого нажатия OK. */
    val confirmingUnlink: Boolean = false,
    val unlinking: Boolean = false,
)

sealed interface DashboardEvent {
    data object Unlinked : DashboardEvent

    /** Бэкенд не видит согласия для этой привязки — показываем экран согласия снова. */
    data object ConsentRequired : DashboardEvent
}
