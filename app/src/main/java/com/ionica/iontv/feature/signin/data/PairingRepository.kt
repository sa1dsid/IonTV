package com.ionica.iontv.feature.signin.data

import com.ionica.iontv.core.api.GONE
import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.NOT_FOUND
import com.ionica.iontv.core.api.UNAUTHORIZED
import com.ionica.iontv.core.api.UNPROCESSABLE
import com.ionica.iontv.core.api.apiError
import com.ionica.iontv.core.api.dto.DeviceDto
import com.ionica.iontv.core.api.dto.PairingPollRequestDto
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.device.DeviceName
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.feature.signin.domain.model.PairingCode
import com.ionica.iontv.feature.signin.domain.model.PairingPoll
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val PAIRING_GONE = setOf(UNAUTHORIZED, NOT_FOUND, GONE, UNPROCESSABLE)

private val MIN_POLL_INTERVAL = 1.seconds

class PairingRepository @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
    private val deviceName: DeviceName,
) {

    suspend fun requestCode(): Result<PairingCode> = runCatchingCancellable {
        val response = api.pairingCode(DeviceDto(sessionStore.deviceId(), deviceName.get()))
        PairingCode(
            pairId = response.pairId,
            code = response.code.uppercase(),
            qrContent = response.qrUrl,
            pollToken = response.pollToken,
            lifetime = response.expiresIn.seconds,
            pollInterval = response.pollInterval.seconds.coerceAtLeast(MIN_POLL_INTERVAL),
        )
    }

    suspend fun poll(pairing: PairingCode): PairingPoll = try {
        val tokens = api.pairingPoll(PairingPollRequestDto(pairing.pairId, pairing.pollToken)).tokensOrNull()
        if (tokens == null) {
            PairingPoll.Pending
        } else {
            sessionStore.open(tokens)
            PairingPoll.Linked
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        if (error.apiError()?.status in PAIRING_GONE) PairingPoll.Expired else PairingPoll.Pending
    }
}
