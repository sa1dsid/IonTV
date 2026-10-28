package com.ionica.iontv.feature.sharing

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import com.ionica.iontv.feature.sharing.service.SharingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowService

@RunWith(RobolectricTestRunner::class)
class SharingServiceTest {

    @Test
    @Config(shadows = [ForegroundDeniedShadow::class])
    fun `a restart the system will not let into the foreground stops quietly instead of crashing`() {
        val service = Robolectric.buildService(SharingService::class.java).create().get()

        val restart = service.onStartCommand(null, 0, 1)

        assertEquals(Service.START_NOT_STICKY, restart)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }
}

@Implements(Service::class)
class ForegroundDeniedShadow : ShadowService() {

    @Implementation
    override fun startForeground(id: Int, notification: Notification, foregroundServiceType: Int) {
        throw ForegroundServiceStartNotAllowedException("Service.startForeground() not allowed due to mAllowStartForeground false")
    }
}
