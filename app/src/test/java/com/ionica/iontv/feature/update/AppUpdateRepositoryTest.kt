package com.ionica.iontv.feature.update

import com.ionica.iontv.core.api.ApiTestCase
import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.feature.update.data.AppUpdateRepository
import com.ionica.iontv.feature.update.domain.model.AppUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CONFIG = "/api/v1/iontv/config"

class AppUpdateRepositoryTest : ApiTestCase() {

    @Test
    fun `a newer version on the server comes back with its links, asked without a token`() = api { stack ->
        stack.linked()
        stack.server.next(CONFIG, body = config(latest = "1.1.0"))

        val update = repository(stack).check().getOrThrow()

        assertEquals(AppUpdate("1.1.0", "https://iontv.ionika.cc/tv", "https://tv.ionika.cc"), update)
        assertEquals(listOf<String?>(null), stack.server.authorizationsOf(CONFIG))
    }

    @Test
    fun `the installed version is not an update`() = api { stack ->
        stack.server.next(CONFIG, body = config(latest = "1.0.0"))

        assertNull(repository(stack).check().getOrThrow())
    }

    @Test
    fun `a config without the android app section is not an update`() = api { stack ->
        stack.server.next(CONFIG, body = """{"sparkCodePrice":240000}""")

        assertNull(repository(stack).check().getOrThrow())
    }

    @Test
    fun `an update without a link to it is not shown`() = api { stack ->
        stack.server.next(CONFIG, body = """{"androidApp":{"latestVersion":"1.1.0","installationUrl":"https://iontv.ionika.cc/tv"}}""")

        assertNull(repository(stack).check().getOrThrow())
    }

    @Test
    fun `a server that does not answer is a failure, not a no`() = api { stack ->
        stack.server.next(CONFIG, code = 503, body = """{"detail":"IONTV is disabled"}""")

        assertTrue(repository(stack).check().isFailure)
    }

    private fun repository(stack: TestApiStack) = AppUpdateRepository(stack.unauthenticatedApi, installedVersion = "1.0.0")
}

internal fun config(latest: String) = """
    {"sparkCodePrice":240000,"androidApp":{"latestVersion":"$latest",
    "installationUrl":"https://iontv.ionika.cc/tv","downloadUrl":"https://tv.ionika.cc"}}
"""
