package com.megamind.yanguservice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.megamind.yanguservice.ui.screen.SendMessageScreen
import com.megamind.yanguservice.ui.theme.YanguServiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YanguServiceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SendMessageScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
