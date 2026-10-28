package com.ionica.iontv.feature.sharing.proxy

import com.ionica.iontv.core.api.ApiTestCase
import com.ionica.iontv.core.api.Server
import com.ionica.iontv.core.api.TestApiStack
import io.proxysdk.HeartbeatRequest
import io.proxysdk.ProxyCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PROXY_HEARTBEAT = "/api/v1/iontv/tv/proxy/heartbeat"

private const val PROXY_DISCONNECT = "/api/v1/iontv/tv/proxy/disconnect"

private const val CERT = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"

class IonTvProxyBackendTest : ApiTestCase() {

    @Test
    fun `the sdk's heartbeat goes out signed by the tv session and its start comes back untouched`() =
        api { stack ->
            stack.linked(access = "tv.access")
            stack.server.next(
                PROXY_HEARTBEAT,
                body = """
                    {"id":"START","proxy_id":42,"host":"203.0.113.7","port":8443,"username":"device",
                    "password":"secret","proxy_version":"1.1","proxy_cert_sha256":"$CERT",
                    "settings":{"v1_1":{"heartbeat_interval_ms":60000}}}
                """,
            )

            val command = backend(stack).heartbeat(request())

            assertEquals(ProxyCommand.Action.START, command?.action)
            assertEquals("203.0.113.7", command?.host)
            assertEquals(8443, command?.port)
            assertEquals("device", command?.username)
            assertEquals("secret", command?.password)
            assertEquals("1.1", command?.protocolVersion)
            assertEquals(CERT, command?.certSha256)
            assertEquals(listOf("Bearer tv.access"), stack.server.authorizationsOf(PROXY_HEARTBEAT))
            assertEquals(
                """{"app_name":"iontv","app_version":"1.0.0","sdk_version":"1.2.1","os_version":"14",""" +
                    """"hardware_id":"a:examplehash","ping":84,"proxy_version":"1.1","proxy_versions":["1.1","1.0"]}""",
                stack.server.bodyOf(PROXY_HEARTBEAT),
            )
        }

    @Test
    fun `fields the sdk does not have yet are left out`() = api { stack ->
        stack.linked()
        stack.server.next(PROXY_HEARTBEAT, body = """{"id":"STOP"}""")

        backend(stack).heartbeat(request(ping = null, hardwareId = null))

        val body = stack.server.bodyOf(PROXY_HEARTBEAT)
        assertFalse(body, body.contains("ping"))
        assertFalse(body, body.contains("hardware_id"))
    }

    @Test
    fun `a stop keeps its reason for the dashboard`() = api { stack ->
        stack.linked()
        stack.server.next(PROXY_HEARTBEAT, body = """{"id":"STOP","reason":"VPN_OR_PROXY"}""")

        val command = backend(stack).heartbeat(request())

        assertEquals(ProxyCommand.Action.STOP, command?.action)
        assertEquals(ProxyCommand.REASON_VPN_OR_PROXY, command?.reason)
    }

    @Test
    fun `a backend that cannot reach the control server is a failed heartbeat`() = api { stack ->
        stack.linked()
        stack.server.next(PROXY_HEARTBEAT, code = 502, body = Server.detail("Proxy control unavailable or invalid response"))

        assertNull(backend(stack).heartbeat(request()))
    }

    @Test
    fun `the sdk is signed in only while the tv is linked`() = api { stack ->
        val backend = backend(stack)
        assertFalse(backend.isSignedIn())

        stack.linked()
        assertTrue(backend.isSignedIn())

        stack.sessionStore.close()
        assertFalse(backend.isSignedIn())
    }

    @Test
    fun `disconnect takes the proxy away with the tv session and an empty body`() = api { stack ->
        stack.linked(access = "tv.access")
        stack.server.next(PROXY_DISCONNECT, body = """{"disconnected":true}""")

        assertTrue(backend(stack).disconnect())

        assertEquals(listOf("Bearer tv.access"), stack.server.authorizationsOf(PROXY_DISCONNECT))
        assertEquals("", stack.server.bodyOf(PROXY_DISCONNECT))
    }

    @Test
    fun `a proxy that was already gone is a done disconnect too`() = api { stack ->
        stack.linked()
        stack.server.next(PROXY_DISCONNECT, body = """{"disconnected":false}""")

        assertTrue(backend(stack).disconnect())
    }

    @Test
    fun `a failed disconnect does not throw`() = api { stack ->
        stack.linked()
        stack.server.next(PROXY_DISCONNECT, code = 502, body = Server.detail("Proxy disconnect unavailable or invalid response"))

        assertFalse(backend(stack).disconnect())
    }

    private fun backend(stack: TestApiStack) = IonTvProxyBackend(stack.api, stack.sessionStore)

    private fun request(ping: Int? = 84, hardwareId: String? = "a:examplehash") = HeartbeatRequest(
        pingMillis = ping,
        protocolVersion = "1.1",
        supportedProtocolVersions = listOf("1.1", "1.0"),
        hardwareId = hardwareId,
        osVersion = "14",
        appName = "iontv",
        appVersion = "1.0.0",
        sdkVersion = "1.2.1",
    )
}
