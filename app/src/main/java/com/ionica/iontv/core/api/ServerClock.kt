package com.ionica.iontv.core.api

import com.ionica.iontv.core.common.TimeSource
import okhttp3.Interceptor
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerClock @Inject constructor(
    private val timeSource: TimeSource,
) {

    @Volatile
    private var offsetMillis = 0L

    fun nowMillis(): Long = timeSource.nowMillis() + offsetMillis

    fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(nowMillis())

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
