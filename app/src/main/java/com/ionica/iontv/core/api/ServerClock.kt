package com.ionica.iontv.core.api

import com.ionica.iontv.core.common.TimeSource
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Часы бэкенда. Heartbeat принимается, только если его время расходится с серверным
 * не больше чем на две минуты, а приставки часто живут с часами, сбитыми на часы и
 * дни. Смещение берём из заголовка Date каждого ответа.
 */
@Singleton
class ServerClock @Inject constructor(
    private val timeSource: TimeSource,
) {

    @Volatile
    private var offsetMillis = 0L

    fun nowMillis(): Long = timeSource.nowMillis() + offsetMillis

    fun onServerTime(serverMillis: Long, receivedAtMillis: Long) {
        offsetMillis = serverMillis - receivedAtMillis
    }
}

@Singleton
class ServerClockInterceptor @Inject constructor(
    private val serverClock: ServerClock,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        response.headers.getDate("Date")?.let { serverDate ->
            serverClock.onServerTime(serverDate.time, response.receivedResponseAtMillis)
        }
        return response
    }
}
