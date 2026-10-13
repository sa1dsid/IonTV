package com.ionica.iontv.feature.sharing.domain.model

import com.ionica.iontv.core.network.Connection

/** Состояние раздачи для дашборда: статус и тип подключения для строки «linked to …». */
data class SharingState(
    val status: SharingStatus,
    val connection: Connection,
)

sealed interface SharingStatus {
    data object Active : SharingStatus

    /** Телевизор на связи, но одно из условий начисления не выполнено. */
    data class Paused(val reason: PauseReason) : SharingStatus

    data object Offline : SharingStatus
}

enum class PauseReason {
    /** Чаще всего: на ТВ снова включилась оптимизация батареи. */
    BATTERY_OPTIMIZATION,

    /** Бэкенд не видит согласия на Data Share для этой привязки. */
    CONSENT_REQUIRED,

    /** Причину бэкенд назвал, но экрана под неё в макете нет. */
    OTHER,
}
