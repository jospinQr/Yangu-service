package com.megamind.yanguservice.ui.screen.splash

import android.window.SplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.megamind.yanguservice.ui.navigation.Splash
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplashScreen(modifier: Modifier = Modifier, onNavigateToHome: () -> Unit) {


    LaunchedEffect(Unit) {
        delay(3.seconds)
        onNavigateToHome()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CircularWavyProgressIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }

}