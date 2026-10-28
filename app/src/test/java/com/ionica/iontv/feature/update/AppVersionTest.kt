package com.ionica.iontv.feature.update

import com.ionica.iontv.feature.update.domain.isNewerVersion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `a higher version on the server is newer`() {
        assertTrue(isNewerVersion("1.1.0", "1.0.0"))
        assertTrue(isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun `parts are compared as numbers, not as text`() {
        assertTrue(isNewerVersion("1.10.0", "1.9.0"))
        assertFalse(isNewerVersion("1.9.0", "1.10.0"))
    }

    @Test
    fun `the same or an older version asks for nothing`() {
        assertFalse(isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(isNewerVersion("1.0", "1.0.0"))
        assertFalse(isNewerVersion("0.9.0", "1.0.0"))
    }

    @Test
    fun `a version we cannot read never asks to update`() {
        assertFalse(isNewerVersion("v1.1.0", "1.0.0"))
        assertFalse(isNewerVersion("1.1.0-beta", "1.0.0"))
        assertFalse(isNewerVersion("", "1.0.0"))
        assertFalse(isNewerVersion("99999999999.0.0", "1.0.0"))
    }
}
