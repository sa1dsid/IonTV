package com.ionica.iontv

import android.app.Application
import com.ionica.iontv.feature.sharing.proxy.SdkProxyClient
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class IonTvApp : Application() {

    @Inject
    lateinit var proxy: SdkProxyClient

    override fun onCreate() {
        super.onCreate()
        proxy.init()
    }
}
