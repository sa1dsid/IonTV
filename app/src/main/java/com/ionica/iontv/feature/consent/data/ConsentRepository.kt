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

const val CONSENT_VERSION = "tv-consent-v1"

object ConsentScreens {
    const val CONSENT = "consent"
    const val DASHBOARD = "dashboard"
}

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

    suspend fun withdraw(): Result<Unit> = runCatchingCancellable {
        record(granted = false, screen = ConsentScreens.DASHBOARD)
    }

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
