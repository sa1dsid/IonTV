package com.ionica.iontv.core.api

import com.ionica.iontv.core.api.dto.HealthDto
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

    /** 204 без тела. Без явного токена отзывает текущую TV-сессию. */
    @POST("api/v1/iontv/auth/logout")
    suspend fun logout(@Header(AUTHORIZATION) authorization: String? = null): Response<Unit>
}

const val AUTHORIZATION = "Authorization"

fun bearer(token: String): String = "Bearer $token"
