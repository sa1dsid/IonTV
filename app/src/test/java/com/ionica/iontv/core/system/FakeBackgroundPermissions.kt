package com.ionica.iontv.core.system

/** Системные настройки в тесте: «открыть» — значит записать, что пользователь туда ушёл. */
internal class FakeBackgroundPermissions(
    var batteryIgnored: Boolean = false,
    var exemptionScreen: Boolean = true,
) : BackgroundPermissions {

    val opened = mutableListOf<String>()

    override fun isBatteryOptimizationIgnored(): Boolean = batteryIgnored

    override fun canRequestBatteryExemption(): Boolean = exemptionScreen

    override fun openBatteryOptimizationSettings(): Boolean {
        opened += BATTERY
        return true
    }

    override fun openAutostartSettings(): Boolean {
        opened += AUTOSTART
        return true
    }

    companion object {
        const val BATTERY = "battery"
        const val AUTOSTART = "autostart"
    }
}
