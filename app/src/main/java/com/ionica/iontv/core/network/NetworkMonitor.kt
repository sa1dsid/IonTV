package com.ionica.iontv.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.ionica.iontv.core.common.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

enum class Connection { ETHERNET, WIFI, OTHER, NONE }

data class NetworkStatus(
    val isOnline: Boolean,
    val connection: Connection,
) {
    companion object {
        val Offline = NetworkStatus(isOnline = false, connection = Connection.NONE)
    }
}

interface NetworkMonitor {
    val status: StateFlow<NetworkStatus>

    fun current(): NetworkStatus
}

@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
) : NetworkMonitor {

    private val connectivity: ConnectivityManager?
        get() = context.getSystemService(ConnectivityManager::class.java)

    override fun current(): NetworkStatus = connectivity.readStatus()

    override val status: StateFlow<NetworkStatus> = callbackFlow {
        val manager = connectivity
        if (manager == null) {
            trySend(NetworkStatus.Offline)
            awaitClose { }
        } else {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(manager.readStatus())
                }

                override fun onLost(network: Network) {
                    trySend(manager.readStatus())
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    trySend(manager.readStatus())
                }
            }

            manager.registerDefaultNetworkCallback(callback)
            trySend(manager.readStatus())

            awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
        }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), current())

    private fun ConnectivityManager?.readStatus(): NetworkStatus {
        val capabilities = this?.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus.Offline
        return NetworkStatus(
            isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            connection = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Connection.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Connection.WIFI
                else -> Connection.OTHER
            },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
