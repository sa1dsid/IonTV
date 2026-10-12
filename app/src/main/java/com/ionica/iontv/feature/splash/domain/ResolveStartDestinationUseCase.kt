package com.ionica.iontv.feature.splash.domain

import com.ionica.iontv.core.session.SessionStore
import javax.inject.Inject

/**
 * Куда вести с заставки. Жива ли привязка, сплэш не спрашивает: если ТВ отвязали,
 * пока он был выключен, первый же heartbeat вернёт его на экран входа.
 */
class ResolveStartDestinationUseCase @Inject constructor(
    private val sessionStore: SessionStore,
) {

    suspend operator fun invoke(): StartDestination = when (sessionStore.current()) {
        null -> StartDestination.SIGN_IN
        else -> StartDestination.DASHBOARD
    }
}
