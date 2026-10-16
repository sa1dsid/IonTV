package com.ionica.iontv.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenRefreshTest : ApiTestCase() {

    @Test
    fun `an expired access token is refreshed once and the call is retried`() = api { stack ->
        stack.linked(access = "tv.access.old", refresh = "tv.refresh.old")
        stack.server.next(Paths.LOGOUT, code = 401, body = Server.detail("Authentication required"))
        stack.server.next(Paths.LOGOUT, code = 204, body = "")
        stack.server.next(Paths.REFRESH, body = Server.tokens(access = "tv.access.new", refresh = "tv.refresh.new"))

        val response = stack.api.logout()

        assertTrue(response.isSuccessful)
        assertEquals(
            listOf("Bearer tv.access.old", "Bearer tv.access.new"),
            stack.server.authorizationsOf(Paths.LOGOUT),
        )
        assertEquals("""{"token":"tv.refresh.old"}""", stack.server.bodyOf(Paths.REFRESH))
        val session = stack.sessionStore.current()
        assertEquals("tv.access.new", session?.accessToken)
        assertEquals("tv.refresh.new", session?.refreshToken)
    }

    @Test
    fun `a rejected refresh token ends the session and issues a new device id`() = api { stack ->
        stack.linked()
        val deviceId = stack.sessionStore.deviceId()
        stack.server.next(Paths.LOGOUT, code = 401, body = Server.detail("Authentication required"))
        stack.server.next(
            Paths.REFRESH,
            code = 401,
            body = Server.error("REFRESH_INVALID", "Invalid refresh token"),
        )

        val response = stack.api.logout()

        assertEquals(401, response.code())
        assertNull(stack.sessionStore.current())
        assertNotEquals(deviceId, stack.sessionStore.deviceId())
    }

    @Test
    fun `a refresh that cannot reach the server keeps the session`() = api { stack ->
        stack.linked()
        stack.server.next(Paths.LOGOUT, code = 401, body = Server.detail("Authentication required"))
        stack.server.next(Paths.REFRESH, code = 503, body = Server.error("IONTV_DISABLED", "IONTV is disabled"))

        val response = stack.api.logout()

        assertFalse(response.isSuccessful)
        assertEquals("tv.access.old", stack.sessionStore.current()?.accessToken)
    }

    @Test
    fun `public endpoints go out without the tv token`() = api { stack ->
        stack.linked()
        stack.server.next(Paths.HEALTH, body = """{"status":"ok","enabled":true}""")

        stack.api.health()

        assertEquals(listOf<String?>(null), stack.server.authorizationsOf(Paths.HEALTH))
    }
}
