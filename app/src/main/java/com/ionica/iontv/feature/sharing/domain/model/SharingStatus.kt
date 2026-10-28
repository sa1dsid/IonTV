package com.ionica.iontv.feature.sharing.domain.model

import com.ionica.iontv.core.network.Connection

data class SharingState(
    val status: SharingStatus,
    val connection: Connection,
)

sealed interface SharingStatus {
    data object Active : SharingStatus

    data class Paused(val reason: PauseReason) : SharingStatus

    data object Offline : SharingStatus
}

enum class PauseReason {
    BATTERY_OPTIMIZATION,

    CONSENT_REQUIRED,

    OTHER,
}
