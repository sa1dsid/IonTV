package com.ionica.iontv.core.api

import com.ionica.iontv.core.api.dto.AccountLoginDto
import com.ionica.iontv.core.api.dto.DeviceDto
import com.ionica.iontv.core.api.dto.GoogleLoginDto
import com.ionica.iontv.core.api.dto.HealthDto
import com.ionica.iontv.core.api.dto.PairingCodeDto
import com.ionica.iontv.core.api.dto.PairingPollDto
import com.ionica.iontv.core.api.dto.PairingPollRequestDto
import com.ionica.iontv.core.api.dto.SessionTokensDto
import com.ionica.iontv.core.api.dto.TokenInputDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Бэкенд IONTV. Запросы с TV-сессией подписывает AuthInterceptor; там, где нужен
 * временный аккаунтный токен (вход через Google или почту), он передаётся явно.
 */
interface IonTvApi {

    @GET("iontv/health")
    suspend fun health(): HealthDto

    @POST("api/v1/iontv/auth/refresh")
    suspend fun refresh(@Body body: TokenInputDto): SessionTokensDto

    /** Пятиминутный код, который вводят в IO Aura (Devices → Add TV) или на сайте Check. */
    @POST("api/v1/iontv/pairing/code")
    suspend fun pairingCode(@Body body: DeviceDto): PairingCodeDto

    @POST("api/v1/iontv/pairing/poll")
    suspend fun pairingPoll(@Body body: PairingPollRequestDto): PairingPollDto

    @POST("api/v1/iontv/auth/google")
    suspend fun googleLogin(@Body body: GoogleLoginDto): AccountLoginDto

    /** Привязывает ТВ аккаунтным токеном и выдаёт взамен ограниченную TV-сессию. */
    @POST("api/v1/iontv/devices/register")
    suspend fun registerDevice(
        @Header(AUTHORIZATION) authorization: String,
        @Body body: DeviceDto,
    ): SessionTokensDto

    /** 204 без тела. Без явного токена отзывает текущую TV-сессию. */
    @POST("api/v1/iontv/auth/logout")
    suspend fun logout(@Header(AUTHORIZATION) authorization: String? = null): Response<Unit>
}

const val AUTHORIZATION = "Authorization"

fun bearer(token: String): String = "Bearer $token"
