package com.megamind.yanguservice.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.megamind.yanguservice.ui.screen.SendMessageScreen
import com.megamind.yanguservice.ui.screen.contacts.ContactScreen
import com.megamind.yanguservice.ui.screen.settings.SettingsScreen
import com.megamind.yanguservice.ui.screen.splash.SplashScreen


data object Splash
data object Settings
data object SenderScreen
data object Contacts


@Composable
fun MyNavDisplay(modifier: Modifier = Modifier) {

    val backStack = remember { mutableStateListOf<Any>(Splash) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Splash -> NavEntry(key) {

                    SplashScreen(
                        onNavigateToHome = {
                            backStack.removeLastOrNull()
                            backStack.add(SenderScreen)
                        }
                    )
                }

                is Settings -> NavEntry(key) { SettingsScreen() }
                is SenderScreen -> NavEntry(key) {
                    SendMessageScreen(onOpenContacts = { backStack.add(Contacts) })
                }
                is Contacts -> NavEntry(key) {
                    ContactScreen(onBack = { backStack.removeLastOrNull() })
                }
                else -> NavEntry(Unit) { Text("Unknown screen") }
            }
        }
    )
}
