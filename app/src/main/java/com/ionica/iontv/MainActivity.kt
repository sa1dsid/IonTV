package com.ionica.iontv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ionica.iontv.core.designsystem.component.IonTvLogo
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IonTvTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(IonTvTheme.colors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    IonTvLogo()
                }
            }
        }
    }
}
