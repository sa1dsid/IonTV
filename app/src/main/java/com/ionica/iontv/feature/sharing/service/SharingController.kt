package com.ionica.iontv.feature.sharing.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Повторный запуск безопасен: сервис не заводит второй цикл heartbeat. */
    fun start() {
        ContextCompat.startForegroundService(context, Intent(context, SharingService::class.java))
    }

    fun stop() {
        context.stopService(Intent(context, SharingService::class.java))
    }
}
