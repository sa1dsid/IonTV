package com.ionica.iontv.core.api

import com.ionica.iontv.core.api.di.createIonTvApi
import com.ionica.iontv.core.api.di.ionTvJson
import com.ionica.iontv.core.common.TimeSource
import com.ionica.iontv.core.session.SessionStore
import okhttp3.OkHttpClient
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

internal class TestApiStack(
    val server: RoutingApiServer,
    val sessionStore: SessionStore,
    timeSource: TimeSource = TimeSource { System.currentTimeMillis() },
) {

    val serverClock = ServerClock(timeSource)

    private val json = ionTvJson()

    private val unauthenticatedClient = OkHttpClient.Builder()
        .dispatcher(okhttp3.Dispatcher(SameThreadExecutorService()))
        .addInterceptor(ServerClockInterceptor(serverClock))
        .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .callTimeout(CALL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .build()

    val unauthenticatedApi: IonTvApi = createIonTvApi(unauthenticatedClient, json, server.url)

    val api: IonTvApi = createIonTvApi(
        client = unauthenticatedClient.newBuilder()
            .addInterceptor(AuthInterceptor(sessionStore))
            .authenticator(TokenAuthenticator(sessionStore, unauthenticatedApi))
            .build(),
        json = json,
        baseUrl = server.url,
    )

    private companion object {
        const val READ_TIMEOUT_MILLIS = 400L
        const val CALL_TIMEOUT_MILLIS = 2_000L
    }
}

private class SameThreadExecutorService : AbstractExecutorService() {

    private var stopped = false

    override fun execute(command: Runnable) = command.run()

    override fun shutdown() {
        stopped = true
    }

    override fun shutdownNow(): MutableList<Runnable> {
        stopped = true
        return mutableListOf()
    }

    override fun isShutdown(): Boolean = stopped

    override fun isTerminated(): Boolean = stopped

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
}
