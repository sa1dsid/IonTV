package com.ionica.iontv.feature.sharing

import com.ionica.iontv.feature.sharing.domain.model.SharingState
import kotlinx.coroutines.flow.Flow

/** То, что дашборд видит от раздачи: статус и способ попросить свежую проверку батареи. */
interface SharingMonitor {

    val state: Flow<SharingState>

    /** Пользователь вернулся из настроек — статус паузы не должен ждать следующей проверки. */
    fun recheckBattery()
}
