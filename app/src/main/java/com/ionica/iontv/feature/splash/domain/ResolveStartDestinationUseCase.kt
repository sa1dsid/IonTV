package com.ionica.iontv.feature.splash.domain

import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.core.system.DeviceSetupStore
import com.ionica.iontv.feature.consent.data.ConsentRepository
import javax.inject.Inject

/**
 * Куда вести с заставки. Жива ли привязка, сплэш не спрашивает: если ТВ отвязали,
 * пока он был выключен, первый же heartbeat вернёт его на экран входа.
 */
class ResolveStartDestinationUseCase @Inject constructor(
    private val sessionStore: SessionStore,
    private val consent: ConsentRepository,
    private val setupStore: DeviceSetupStore,
) {

    suspend operator fun invoke(): StartDestination = when {
        sessionStore.current() == null -> StartDestination.SIGN_IN
        // Приложение вышло из процесса между привязкой и согласием — спрашиваем снова.
        !consent.isGranted() -> StartDestination.CONSENT
        !setupStore.isSetupCompleted() -> StartDestination.SETUP
        else -> StartDestination.DASHBOARD
    }
}
