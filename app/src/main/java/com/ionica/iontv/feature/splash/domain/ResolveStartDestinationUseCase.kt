package com.ionica.iontv.feature.splash.domain

import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.core.system.DeviceSetupStore
import com.ionica.iontv.feature.consent.data.ConsentRepository
import javax.inject.Inject

class ResolveStartDestinationUseCase @Inject constructor(
    private val sessionStore: SessionStore,
    private val consent: ConsentRepository,
    private val setupStore: DeviceSetupStore,
) {

    suspend operator fun invoke(): StartDestination = when {
        sessionStore.current() == null -> StartDestination.SIGN_IN
        !consent.isGranted() -> StartDestination.CONSENT
        !setupStore.isSetupCompleted() -> StartDestination.SETUP
        else -> StartDestination.DASHBOARD
    }
}
