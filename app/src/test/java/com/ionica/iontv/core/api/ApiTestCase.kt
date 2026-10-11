package com.ionica.iontv.core.api

import com.ionica.iontv.core.session.SessionTokens
import com.ionica.iontv.testing.sessionStoreIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class ApiTestCase {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun closeStores() {
        storeScope.cancel()
    }

    internal fun api(body: suspend (TestApiStack) -> Unit) = runBlocking {
        val server = RoutingApiServer()
        try {
            body(TestApiStack(server, sessionStoreIn(folder.root, storeScope)))
        } finally {
            server.shutdown()
        }
    }

    internal suspend fun TestApiStack.linked(
        access: String = "tv.access.old",
        refresh: String = "tv.refresh.old",
        username: String? = null,
    ) {
        sessionStore.open(SessionTokens(access, refresh, BINDING_ID), username)
    }

    internal companion object {
        const val BINDING_ID = "4a0c38c8-6a5e-4b67-9a0e-1b5c7f2e9d11"
    }
}
