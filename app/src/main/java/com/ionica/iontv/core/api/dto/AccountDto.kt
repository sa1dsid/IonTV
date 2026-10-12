package com.ionica.iontv.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val CLIENT_NATIVE = "native"

/**
 * Общий вход IONTV отвечает в camelCase. clientType=native: refresh-токен приходит
 * в JSON, а не в HttpOnly-cookie, как у сайта Check.
 */
@Serializable
data class GoogleLoginDto(
    @SerialName("idToken") val idToken: String,
    @SerialName("clientType") val clientType: String = CLIENT_NATIVE,
)

@Serializable
data class EmailInputDto(
    @SerialName("email") val email: String,
)

@Serializable
data class EmailCodeSentDto(
    @SerialName("resendAfterSeconds") val resendAfterSeconds: Int = 60,
)

/** Шестизначный код из письма: одноразовый, живёт десять минут, пять ошибок — и он сгорает. */
@Serializable
data class EmailLoginDto(
    @SerialName("email") val email: String,
    @SerialName("code") val code: String,
    @SerialName("clientType") val clientType: String = CLIENT_NATIVE,
)

@Serializable
data class AccountLoginDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("isNewUser") val isNewUser: Boolean = false,
    @SerialName("user") val user: AccountUserDto,
)

@Serializable
data class AccountUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String = "",
)
