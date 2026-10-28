package com.ionica.iontv.core.session

import com.ionica.iontv.testing.preferencesIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the device id survives an app restart`() = runBlocking {
        val first = withStore { it.deviceId() }
        val second = withStore { it.deviceId() }

        assertEquals(first, second)
    }

    @Test
    fun `a linked session comes back after a restart`() = runBlocking {
        withStore { it.open(SessionTokens("access", "refresh", "binding")) }

        val session = withStore { it.session.first() }

        assertEquals(TvSession("access", "refresh", "binding"), session)
    }

    @Test
    fun `closing the session forgets the tokens and the old device id`() = runBlocking {
        val deviceId = withStore { store ->
            store.open(SessionTokens("access", "refresh", "binding"))
            store.deviceId().also { store.close() }
        }

        withStore { store ->
            assertNull(store.current())
            assertNotEquals(deviceId, store.deviceId())
        }
    }

    private suspend fun <T> withStore(block: suspend (SessionStore) -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return try {
            block(SessionStore(preferencesIn(folder.root, "session", scope)))
        } finally {
            scope.coroutineContext.job.cancelAndJoin()
        }
    }
}
