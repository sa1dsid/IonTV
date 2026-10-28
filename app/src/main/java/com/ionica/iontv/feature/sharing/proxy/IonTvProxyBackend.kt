package com.ionica.iontv.feature.sharing.proxy

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.dto.ProxyHeartbeatDto
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.session.SessionStore
import io.proxysdk.HeartbeatRequest
import io.proxysdk.ProxyBackend
import io.proxysdk.ProxyCommand
import javax.inject.Inject

class IonTvProxyBackend @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
) : ProxyBackend {

    override suspend fun heartbeat(request: HeartbeatRequest): ProxyCommand? =
        runCatchingCancellable {
            ProxyCommand.fromJson(api.proxyHeartbeat(request.toDto()).string())
        }.getOrNull()

    override fun isSignedIn(): Boolean = sessionStore.blockingCurrent() != null

    override suspend fun disconnect(): Boolean =
        runCatchingCancellable { api.proxyDisconnect() }.isSuccess

    private fun HeartbeatRequest.toDto() = ProxyHeartbeatDto(
        appName = appName,
        appVersion = appVersion,
        sdkVersion = sdkVersion,
        osVersion = osVersion,
        hardwareId = hardwareId,
        ping = pingMillis,
        proxyVersion = protocolVersion,
        proxyVersions = supportedProtocolVersions,
    )
}
