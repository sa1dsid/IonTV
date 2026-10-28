package com.ionica.iontv.core.api

import com.ionica.iontv.core.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

private val PUBLIC_PATHS = setOf(
    "/iontv/health",
    "/api/v1/iontv/auth/refresh",
    "/api/v1/iontv/pairing/code",
    "/api/v1/iontv/pairing/poll",
)

class TvSessionAuth(val accessToken: String)

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath in PUBLIC_PATHS) return chain.proceed(request)

        val session = sessionStore.blockingCurrent() ?: return chain.proceed(request)
        return chain.proceed(request.signedWith(session.accessToken))
    }
}

internal fun Request.signedWith(accessToken: String): Request = newBuilder()
    .header(AUTHORIZATION, bearer(accessToken))
    .tag(TvSessionAuth::class.java, TvSessionAuth(accessToken))
    .build()
