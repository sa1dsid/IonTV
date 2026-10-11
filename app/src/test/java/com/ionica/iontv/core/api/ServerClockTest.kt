package com.ionica.iontv.core.api

import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private const val HOUR_MILLIS = 3_600_000L

private const val TOLERANCE_MILLIS = 2_000L

class ServerClockTest : ApiTestCase() {

    @Test
    fun `the tv takes the server time from the Date header`() = api { stack ->
        val serverNow = System.currentTimeMillis() + HOUR_MILLIS
        stack.server.next(
            Paths.HEALTH,
            body = """{"status":"ok","enabled":true}""",
            headers = mapOf("Date" to httpDate(serverNow)),
        )

        stack.api.health()

        val drift = stack.serverClock.nowMillis() - serverNow
        assertTrue("drift $drift ms", abs(drift) < TOLERANCE_MILLIS)
    }

    private fun httpDate(millis: Long): String =
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("GMT") }
            .format(millis)
}
