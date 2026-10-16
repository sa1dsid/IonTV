package com.ionica.iontv.feature.consent.data

import android.os.Build
import com.ionica.iontv.BuildConfig
import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.ServerClock
import com.ionica.iontv.core.api.dto.ConsentDeviceInfoDto
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

// Бэкенд принимает в снимке устройства не больше 100 символов на поле.
private const val DEVICE_INFO_MAX_LENGTH = 100

class ConsentRepository @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
    private val serverClock: ServerClock,
) {

    suspend fun isGranted(): Boolean = sessionStore.consentVersion() == CONSENT_VERSION

    suspend fun grant(): Result<Unit> = runCatchingCancellable {
        record(granted = true, screen = ConsentScreens.CONSENT)
        sessionStore.saveConsent(CONSENT_VERSION)
    }

    /** Отзыв при отвязке с телевизора. */
    suspend fun withdraw(): Result<Unit> = runCatchingCancellable {
        record(granted = false, screen = ConsentScreens.DASHBOARD)
    }

    // Запись для аудита покупателей трафика: что показали, где, когда и на каком устройстве.
    private suspend fun record(granted: Boolean, screen: String) {
        api.consent(
            deviceId = sessionStore.deviceId(),
            body = ConsentDto(
                granted = granted,
                version = CONSENT_VERSION,
                screen = screen,
                timestamp = serverClock.nowIso(),
                deviceInfo = deviceInfo(),
            ),
        )
    }

    private fun deviceInfo() = ConsentDeviceInfoDto(
        manufacturer = Build.MANUFACTURER.limited(),
        model = Build.MODEL.limited(),
        osVersion = Build.VERSION.RELEASE.limited(),
        appVersion = BuildConfig.VERSION_NAME.limited(),
    )

    private fun String?.limited(): String? = this?.trim()?.take(DEVICE_INFO_MAX_LENGTH)?.ifBlank { null }
}
