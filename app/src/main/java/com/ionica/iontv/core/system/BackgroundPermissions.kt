package com.ionica.iontv.core.system

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Разрешения, без которых телевизор не раздаёт трафик в фоне и после перезагрузки. */
interface BackgroundPermissions {

    /** battery_ok из бэкенд-ТЗ: без него начисление на бэкенде останавливается. */
    fun isBatteryOptimizationIgnored(): Boolean

    /** Есть ли на этом устройстве экран, где оптимизацию батареи можно снять. */
    fun canRequestBatteryExemption(): Boolean

    fun openBatteryOptimizationSettings(): Boolean

    fun openAutostartSettings(): Boolean
}

// Менеджеры автозапуска китайских оболочек: системного статуса у них нет, поэтому
// по ТЗ шаг считается выполненным, когда пользователь сходил в настройки и вернулся.
private val AUTOSTART_SCREENS = listOf(
    ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
    ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
    ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
    ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"),
)

private const val STUB_ACTIVITY_SUFFIX = "StubActivity"

@Singleton
class AndroidBackgroundPermissions @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BackgroundPermissions {

    // До Android 6 оптимизации батареи нет вовсе.
    override fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    override fun canRequestBatteryExemption(): Boolean = batteryScreens().isNotEmpty()

    override fun openBatteryOptimizationSettings(): Boolean =
        batteryScreens().any { startSafely(it) } || openAppDetails() || startSafely(Intent(Settings.ACTION_SETTINGS))

    override fun openAutostartSettings(): Boolean =
        AUTOSTART_SCREENS.any { startSafely(Intent().setComponent(it)) } ||
            openAppDetails() ||
            startSafely(Intent(Settings.ACTION_SETTINGS))

    // Сначала системный диалог «разрешить работу в фоне» прямо для нашего пакета,
    // потом общий список исключений. На многих телевизорах нет ни того, ни другого.
    private fun batteryScreens(): List<Intent> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
        return listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        ).filter { it.opensRealScreen() }
    }

    private fun openAppDetails(): Boolean =
        startSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))

    private fun packageUri(): Uri = Uri.parse("package:${context.packageName}")

    // Android TV и Google TV принимают телефонные интенты настроек, но отдают их заглушке
    // EmptyStubActivity: она открывается и сразу закрывается. Такой экран — всё равно что
    // никакого, иначе галка «разрешить работу в фоне» не загорится никогда.
    private fun Intent.opensRealScreen(): Boolean {
        val activity = context.packageManager
            .resolveActivity(this, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?: return false
        return !activity.name.endsWith(STUB_ACTIVITY_SUFFIX)
    }

    private fun startSafely(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (notFound: ActivityNotFoundException) {
        false
    } catch (denied: SecurityException) {
        false
    }
}
