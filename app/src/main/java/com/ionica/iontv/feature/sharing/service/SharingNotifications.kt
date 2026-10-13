package com.ionica.iontv.feature.sharing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ionica.iontv.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "data_share"

/**
 * Уведомление foreground-сервиса. Телевизоры их почти не показывают, но без него
 * система не даст сервису жить в фоне.
 */
@Singleton
class SharingNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun build(): Notification {
        createChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.sharing_notification_text))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.sharing_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }
}
