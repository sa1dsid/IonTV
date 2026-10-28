package com.ionica.iontv.feature.sharing

import com.ionica.iontv.feature.sharing.proxy.ProxyClient
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeProxyClient(serving: Boolean = true) : ProxyClient {

    override val serving = MutableStateFlow(serving)

    override val stopReason = MutableStateFlow<String?>(null)

    var starts = 0
        private set

    var stops = 0
        private set

    override fun isServing(): Boolean = serving.value

    override fun start() {
        starts++
    }

    override fun stop() {
        stops++
    }
}
