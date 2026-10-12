package com.ionica.iontv.core.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_NAME_LENGTH = 100

private const val FALLBACK_NAME = "Android TV"

/** Имя телевизора для списка устройств в IO Aura и Check — например «Living room TV». */
fun interface DeviceName {
    fun get(): String
}

@Singleton
class SystemDeviceName @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceName {

    // Имя из настроек ТВ пользователь узнает в списке; модель — запасной вариант
    // для приставок, где имя не задано.
    override fun get(): String {
        val userName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } else {
            null
        }
        val name = userName?.trim()?.ifBlank { null } ?: modelName()
        return name.take(MAX_NAME_LENGTH).ifBlank { FALLBACK_NAME }
    }

    private fun modelName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val name = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        return name.trim().replaceFirstChar { it.uppercase() }
    }
}
