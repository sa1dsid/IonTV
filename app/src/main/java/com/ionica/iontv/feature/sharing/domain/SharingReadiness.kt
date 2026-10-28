package com.ionica.iontv.feature.sharing.domain

import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.core.system.DeviceSetupStore
import com.ionica.iontv.feature.consent.data.ConsentRepository
import javax.inject.Inject

class SharingReadiness @Inject constructor(
    private val sessionStore: SessionStore,
    private val consent: ConsentRepository,
    private val setupStore: DeviceSetupStore,
) {

    suspend fun isReady(): Boolean =
        sessionStore.current() != null && consent.isGranted() && setupStore.isSetupCompleted()
}
