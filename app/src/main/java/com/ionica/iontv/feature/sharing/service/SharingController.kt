package com.ionica.iontv.feature.sharing.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SharingController {

    /** Повторный запуск безопасен: сервис не заводит второй цикл heartbeat. */
    fun start()

    fun stop()
}

@Singleton
class ServiceSharingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SharingController {

    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, SharingService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, SharingService::class.java))
    }
}
