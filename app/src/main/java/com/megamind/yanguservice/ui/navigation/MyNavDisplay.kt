package com.megamind.yanguservice.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.ui.screen.SendMessageScreen
import com.megamind.yanguservice.ui.screen.contacts.ContactScreen
import com.megamind.yanguservice.ui.screen.settings.SettingsScreen
import com.megamind.yanguservice.ui.screen.splash.SplashScreen


data object Splash
data object Settings
data object SenderScreen
data object Contacts
data class SendToContacts(val contacts: List<Contact>)


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

                is Settings -> NavEntry(key) {
                    SettingsScreen(onBack = { backStack.removeLastOrNull() })
                }
                is SenderScreen -> NavEntry(key) {
                    SendMessageScreen(
                        onOpenContacts = { backStack.add(Contacts) },
                        onOpenSettings = { backStack.add(Settings) },
                    )
                }
                is Contacts -> NavEntry(key) {
                    ContactScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onSendSelected = { selected -> backStack.add(SendToContacts(selected)) },
                    )
                }
                is SendToContacts -> NavEntry(key) {
                    SendMessageScreen(
                        selectedContacts = key.contacts,
                        onOpenContacts = { backStack.removeLastOrNull() },
                        onOpenSettings = { backStack.add(Settings) },
                    )
                }
                else -> NavEntry(Unit) { Text("Unknown screen") }
            }
        }
    )
}
