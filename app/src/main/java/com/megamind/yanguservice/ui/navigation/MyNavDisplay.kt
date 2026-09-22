package com.megamind.yanguservice.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.megamind.yanguservice.ui.screen.SendMessageScreen
import com.megamind.yanguservice.ui.screen.SenderViewModel
import com.megamind.yanguservice.ui.screen.contacts.ContactScreen
import com.megamind.yanguservice.ui.screen.settings.SettingsScreen
import com.megamind.yanguservice.ui.screen.splash.SplashScreen
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.collectAsState


data object Splash
data object Settings
data object SenderScreen
data object Contacts


@Composable
fun MyNavDisplay(modifier: Modifier = Modifier) {

    val backStack = remember { mutableStateListOf<Any>(Splash) }
    val senderViewModel: SenderViewModel = koinViewModel()

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
                        viewModel = senderViewModel,
                    )
                }

                is Contacts -> NavEntry(key) {
                    ContactScreen(
                        onBack = { backStack.removeLastOrNull() },
                        initialSelectedContactIds = senderViewModel.uiState.collectAsState().value.selectedContacts
                            .mapTo(HashSet()) { it.id },
                        onSendSelected = { selected ->
                            senderViewModel.setSelectedContacts(selected)
                            backStack.removeLastOrNull()
                        },
                    )
                }

                else -> NavEntry(Unit) { Text("Unknown screen") }
            }
        }
    )
}
