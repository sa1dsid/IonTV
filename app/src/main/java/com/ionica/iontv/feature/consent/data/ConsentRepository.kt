package com.ionica.iontv.feature.consent.data

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.dto.ConsentDto
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.session.SessionStore
import javax.inject.Inject

/**
 * Версия текста согласия с экрана 3. Меняется вместе с текстом: тогда бэкенд увидит
 * новую версию в аудите, а телевизор попросит согласиться заново.
 */
const val CONSENT_VERSION = "tv-consent-v1"

/** Экраны, где согласие дают или отзывают, — для аудита на бэкенде. */
object ConsentScreens {
    const val CONSENT = "consent"
    const val DASHBOARD = "dashboard"
}

class ConsentRepository @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
) {

    suspend fun isGranted(): Boolean = sessionStore.consentVersion() == CONSENT_VERSION

    suspend fun grant(): Result<Unit> = runCatchingCancellable {
        api.consent(
            deviceId = sessionStore.deviceId(),
            body = ConsentDto(granted = true, version = CONSENT_VERSION, screen = ConsentScreens.CONSENT),
        )
        sessionStore.saveConsent(CONSENT_VERSION)
    }
}
