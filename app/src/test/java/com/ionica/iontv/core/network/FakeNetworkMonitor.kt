package com.ionica.iontv.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeNetworkMonitor(
    initial: NetworkStatus = NetworkStatus(isOnline = true, connection = Connection.ETHERNET),
) : NetworkMonitor {

    private val flow = MutableStateFlow(initial)

    override val status: StateFlow<NetworkStatus> = flow

    override fun current(): NetworkStatus = flow.value

    fun switchTo(status: NetworkStatus) {
        flow.value = status
    }
}
