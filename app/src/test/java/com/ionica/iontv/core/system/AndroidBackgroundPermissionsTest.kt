package com.ionica.iontv.core.system

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidBackgroundPermissionsTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

    private val permissions = AndroidBackgroundPermissions(application)

    @Test
    fun `a tv that hands battery settings to a stub has no exemption screen`() {
        handle(requestExemption(), activity = "com.android.tv.settings.EmptyStubActivity")
        handle(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), activity = "com.android.tv.settings.EmptyStubActivity")

        assertFalse(permissions.canRequestBatteryExemption())

        assertTrue(permissions.openBatteryOptimizationSettings())
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, startedAction())
    }

    @Test
    fun `a box with the system dialog asks for the exemption directly`() {
        handle(requestExemption(), activity = "com.android.settings.fuelgauge.RequestIgnoreBatteryOptimizations")

        assertTrue(permissions.canRequestBatteryExemption())

        assertTrue(permissions.openBatteryOptimizationSettings())
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, startedAction())
    }

    @Test
    fun `a box without any battery screen has no exemption screen`() {
        assertFalse(permissions.canRequestBatteryExemption())
    }

    private fun requestExemption() = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${application.packageName}"),
    )

    private fun handle(intent: Intent, activity: String) {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = activity.substringBeforeLast('.')
                name = activity
            }
        }
        shadowOf(application.packageManager).addResolveInfoForIntent(intent, resolveInfo)
    }

    private fun startedAction(): String? = shadowOf(application).nextStartedActivity?.action
}
