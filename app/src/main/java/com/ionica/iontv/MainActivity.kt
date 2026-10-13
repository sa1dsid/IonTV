package com.ionica.iontv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.navigation.IonTvRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IonTvTheme {
                IonTvRoot(onExit = ::finish)
            }
        }
    }
}
