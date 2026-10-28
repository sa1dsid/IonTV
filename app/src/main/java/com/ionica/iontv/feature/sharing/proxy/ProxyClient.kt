package com.ionica.iontv.feature.sharing.proxy

import android.content.Context
import com.ionica.iontv.MainActivity
import com.ionica.iontv.R
import com.ionica.iontv.feature.sharing.service.SharingNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import io.proxysdk.ProxyNotification
import io.proxysdk.ProxySdk
import io.proxysdk.ProxySdkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val PROXY_APP_NAME = "iontv"

private const val PROXY_NOTIFICATION_ID = 2

interface ProxyClient {

    fun isServing(): Boolean

    val serving: Flow<Boolean>

    val stopReason: Flow<String?>

    fun start()

    fun stop()
}

@Singleton
class SdkProxyClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backend: IonTvProxyBackend,
) : ProxyClient {

    fun init() {
        ProxySdk.init(
            context,
            ProxySdkConfig(appName = PROXY_APP_NAME, backend = backend, notification = notification()),
        )
    }

    override fun isServing(): Boolean = ProxySdk.isServing()

    override val serving: Flow<Boolean>
        get() = ProxySdk.heartbeat.map { ProxySdk.isServing() }.distinctUntilChanged()

    override val stopReason: Flow<String?>
        get() = ProxySdk.stopReason

    override fun start() = ProxySdk.start(context)

    override fun stop() = ProxySdk.stop(context)

    private fun notification() = ProxyNotification(
        smallIcon = R.drawable.ic_notification,
        channelName = context.getString(R.string.sharing_notification_channel),
        openActivity = MainActivity::class.java,
        title = context.getString(R.string.app_name),
        text = context.getString(R.string.sharing_notification_text),
        channelId = SharingNotifications.CHANNEL_ID,
        notificationId = PROXY_NOTIFICATION_ID,
    )
}
