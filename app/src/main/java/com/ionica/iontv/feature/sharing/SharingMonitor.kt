package com.ionica.iontv.feature.sharing

import com.ionica.iontv.feature.sharing.domain.model.SharingState
import kotlinx.coroutines.flow.Flow

interface SharingMonitor {

    val state: Flow<SharingState>

    fun recheckBattery()

    fun refresh()
}
