package com.ionica.iontv.core.session

data class TvSession(
    val accessToken: String,
    val refreshToken: String,
    val bindingId: String?,
)

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
    val bindingId: String?,
)
