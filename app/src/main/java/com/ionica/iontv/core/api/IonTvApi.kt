package com.ionica.iontv.core.api

import com.ionica.iontv.core.api.dto.ConfigDto
import com.ionica.iontv.core.api.dto.ConsentDto
import com.ionica.iontv.core.api.dto.ConsentStateDto
import com.ionica.iontv.core.api.dto.DeviceDto
import com.ionica.iontv.core.api.dto.HealthDto
import com.ionica.iontv.core.api.dto.HeartbeatDto
import com.ionica.iontv.core.api.dto.HeartbeatResultDto
import com.ionica.iontv.core.api.dto.PairingCodeDto
import com.ionica.iontv.core.api.dto.PairingPollDto
import com.ionica.iontv.core.api.dto.PairingPollRequestDto
import com.ionica.iontv.core.api.dto.ProxyDisconnectedDto
import com.ionica.iontv.core.api.dto.ProxyHeartbeatDto
import com.ionica.iontv.core.api.dto.SessionTokensDto
import com.ionica.iontv.core.api.dto.TokenInputDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface IonTvApi {

    @GET("iontv/health")
    suspend fun health(): HealthDto

    @GET("api/v1/iontv/config")
    suspend fun config(): ConfigDto

    @POST("api/v1/iontv/auth/refresh")
    suspend fun refresh(@Body body: TokenInputDto): SessionTokensDto

    @POST("api/v1/iontv/pairing/code")
    suspend fun pairingCode(@Body body: DeviceDto): PairingCodeDto

    @POST("api/v1/iontv/pairing/poll")
    suspend fun pairingPoll(@Body body: PairingPollRequestDto): PairingPollDto

    @POST("api/v1/iontv/device/{deviceId}/consent")
    suspend fun consent(
        @Path("deviceId") deviceId: String,
        @Body body: ConsentDto,
    ): ConsentStateDto

    @POST("api/v1/iontv/tv/heartbeat")
    suspend fun heartbeat(@Body body: HeartbeatDto): HeartbeatResultDto

    @POST("api/v1/iontv/tv/proxy/heartbeat")
    suspend fun proxyHeartbeat(@Body body: ProxyHeartbeatDto): ResponseBody

    @POST("api/v1/iontv/tv/proxy/disconnect")
    suspend fun proxyDisconnect(): ProxyDisconnectedDto

    @POST("api/v1/iontv/auth/logout")
    suspend fun logout(): Response<Unit>
}

const val AUTHORIZATION = "Authorization"

fun bearer(token: String): String = "Bearer $token"
