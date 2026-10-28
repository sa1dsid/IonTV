package com.ionica.iontv.feature.sharing.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ionica.iontv.feature.sharing.proxy.ProxyClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SharingController {

    fun start()

    fun stop()
}

@Singleton
class ServiceSharingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val proxy: ProxyClient,
) : SharingController {

    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, SharingService::class.java))
        proxy.start()
    }

    override fun stop() {
        proxy.stop()
        context.stopService(Intent(context, SharingService::class.java))
    }
}
