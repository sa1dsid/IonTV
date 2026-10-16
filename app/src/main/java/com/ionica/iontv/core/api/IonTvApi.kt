package com.ionica.iontv.core.api

import com.ionica.iontv.core.api.dto.ConsentDto
import com.ionica.iontv.core.api.dto.ConsentStateDto
import com.ionica.iontv.core.api.dto.DeviceDto
import com.ionica.iontv.core.api.dto.HealthDto
import com.ionica.iontv.core.api.dto.HeartbeatDto
import com.ionica.iontv.core.api.dto.HeartbeatResultDto
import com.ionica.iontv.core.api.dto.PairingCodeDto
import com.ionica.iontv.core.api.dto.PairingPollDto
import com.ionica.iontv.core.api.dto.PairingPollRequestDto
import com.ionica.iontv.core.api.dto.SessionTokensDto
import com.ionica.iontv.core.api.dto.TokenInputDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Бэкенд IONTV для телевизора. Входа на ТВ нет: TV-сессию он получает только привязкой
 * по QR, а запросы с ней подписывает AuthInterceptor.
 */
interface IonTvApi {

    @GET("iontv/health")
    suspend fun health(): HealthDto

    @POST("api/v1/iontv/auth/refresh")
    suspend fun refresh(@Body body: TokenInputDto): SessionTokensDto

    /**
     * Пятиминутная привязка: QR со ссылкой на сайт IonTV Pulse и короткий код для ручного
     * ввода. Подтверждает её залогиненный пользователь на сайте.
     */
    @POST("api/v1/iontv/pairing/code")
    suspend fun pairingCode(@Body body: DeviceDto): PairingCodeDto

    @POST("api/v1/iontv/pairing/poll")
    suspend fun pairingPoll(@Body body: PairingPollRequestDto): PairingPollDto

    /** TV-сессия может менять согласие только своего телевизора. */
    @POST("api/v1/iontv/device/{deviceId}/consent")
    suspend fun consent(
        @Path("deviceId") deviceId: String,
        @Body body: ConsentDto,
    ): ConsentStateDto

    /** Только TV-сессией. В ответе — привязан ли ещё ТВ и как бэкенд видит его статус. */
    @POST("api/v1/iontv/tv/heartbeat")
    suspend fun heartbeat(@Body body: HeartbeatDto): HeartbeatResultDto

    /** 204 без тела. Отзывает текущую TV-сессию. */
    @POST("api/v1/iontv/auth/logout")
    suspend fun logout(): Response<Unit>
}

const val AUTHORIZATION = "Authorization"

fun bearer(token: String): String = "Bearer $token"
