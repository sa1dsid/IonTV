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

// Протухший или чужой код: Swagger обещает 401 и 410, живой сервер на неизвестный
// код отвечает 404. Для экрана всё одно — нужен новый код.
private val CODE_GONE = setOf(UNAUTHORIZED, NOT_FOUND, GONE, UNPROCESSABLE)

/**
 * Привязка по коду: ТВ берёт код, пользователь вводит его в IO Aura или на сайте Check,
 * а ТВ опрашивает бэкенд, пока не получит свою сессию.
 */
class PairingRepository @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
    private val deviceName: DeviceName,
) {

    suspend fun requestCode(): Result<PairingCode> = runCatchingCancellable {
        val response = api.pairingCode(DeviceDto(sessionStore.deviceId(), deviceName.get()))
        PairingCode(
            code = response.code,
            pollToken = response.pollToken,
            lifetime = response.expiresIn.seconds,
        )
    }

    suspend fun poll(code: PairingCode): PairingPoll = try {
        val tokens = api.pairingPoll(PairingPollRequestDto(code.code, code.pollToken)).tokensOrNull()
        if (tokens == null) {
            PairingPoll.Pending
        } else {
            sessionStore.open(tokens, username = null)
            PairingPoll.Linked
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        if (error.apiError()?.status in CODE_GONE) PairingPoll.Expired else PairingPoll.Pending
    }
}
