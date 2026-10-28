package com.ionica.iontv.core.api

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy

internal class RoutingApiServer {

    private val replies = mutableMapOf<String, ArrayDeque<MockResponse>>()
    private val standing = mutableMapOf<String, MockResponse>()
    private val recorded = mutableListOf<RecordedRequest>()

    private val server = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(this@RoutingApiServer) {
                    recorded += request
                    val path = request.path?.substringBefore('?').orEmpty()
                    val route = routeOf(request.method, path)
                    return replies[route]?.removeFirstOrNull()
                        ?: replies[path]?.removeFirstOrNull()
                        ?: standing[route]
                        ?: standing[path]
                        ?: MockResponse().setResponseCode(404).setBody("""{"detail":"no stub"}""")
                }
            }
        }
    }

    val url: String
        get() = server.url("/").toString()

    fun always(path: String, code: Int = 200, body: String = "{}", method: String? = null) =
        synchronized(this) {
            standing[routeOf(method, path)] = json(code, body)
        }

    fun next(
        path: String,
        code: Int = 200,
        body: String = "{}",
        method: String? = null,
        headers: Map<String, String> = emptyMap(),
    ) = synchronized(this) {
        val response = json(code, body).apply { headers.forEach { (name, value) -> setHeader(name, value) } }
        replies.getOrPut(routeOf(method, path)) { ArrayDeque() } += response
    }

    fun nextDropsConnection(path: String, method: String? = null) = synchronized(this) {
        replies.getOrPut(routeOf(method, path)) { ArrayDeque() } +=
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
    }

    fun requests(): List<RecordedRequest> = synchronized(this) { recorded.toList() }

    fun paths(): List<String> = requests().mapNotNull { it.path?.substringBefore('?') }

    fun hits(path: String, method: String? = null): Int = matching(path, method).size

    fun bodyOf(path: String, method: String? = null): String =
        matching(path, method).last().body.clone().readUtf8()

    fun bodiesOf(path: String, method: String? = null): List<String> =
        matching(path, method).map { it.body.clone().readUtf8() }

    fun authorizationsOf(path: String, method: String? = null): List<String?> =
        matching(path, method).map { it.getHeader(AUTHORIZATION) }

    fun shutdown() = server.shutdown()

    private fun matching(path: String, method: String?): List<RecordedRequest> = requests()
        .filter { it.path?.substringBefore('?') == path }
        .filter { method == null || it.method == method }

    private fun routeOf(method: String?, path: String): String =
        if (method == null) path else "$method $path"

    private fun json(code: Int, body: String) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
