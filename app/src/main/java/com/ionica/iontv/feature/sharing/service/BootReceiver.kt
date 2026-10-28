package com.ionica.iontv.feature.sharing.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ionica.iontv.core.common.ApplicationScope
import com.ionica.iontv.feature.sharing.domain.SharingReadiness
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private val STARTING_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
)

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var readiness: SharingReadiness

    @Inject
    lateinit var controller: SharingController

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in STARTING_ACTIONS) return

        val pending = goAsync()
        scope.launch {
            try {
                if (readiness.isReady()) controller.start()
            } finally {
                pending.finish()
            }
        }
    }
}
