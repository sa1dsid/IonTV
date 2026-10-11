package com.ionica.iontv.core.api

internal object Paths {
    const val HEALTH = "/iontv/health"
    const val REFRESH = "/api/v1/iontv/auth/refresh"
    const val LOGOUT = "/api/v1/iontv/auth/logout"
}

internal object Server {

    fun tokens(
        access: String = "tv.access.token",
        refresh: String = "tv.refresh.token",
        bindingId: String? = "4a0c38c8-6a5e-4b67-9a0e-1b5c7f2e9d11",
    ): String = """
        {"access_token":"$access","refresh_token":"$refresh","token_type":"bearer",
        "expires_in":900,"binding_id":${bindingId?.let { "\"$it\"" } ?: "null"}}
    """

    fun detail(text: String): String = """{"detail":"$text"}"""

    fun error(code: String, message: String): String = """{"code":"$code","message":"$message"}"""
}
