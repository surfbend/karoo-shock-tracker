package com.shocktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.shocktracker.repository.MaintenanceRepository
import com.shocktracker.ui.screens.BikeDetailScreen
import com.shocktracker.ui.screens.BikeListScreen
import com.shocktracker.ui.screens.SettingsScreen
import com.shocktracker.ui.theme.ShockTrackerTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: MaintenanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MaintenanceRepository(this)

        setContent {
            ShockTrackerTheme {
                AppNavigation(repository)
            }
        }
    }
}

sealed class Screen {
    object BikeList : Screen()
    data class BikeDetail(val bikeId: String) : Screen()
    object Settings : Screen()
}

@Composable
fun AppNavigation(repository: MaintenanceRepository) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.BikeList) }

    when (val screen = currentScreen) {
        is Screen.BikeList -> BikeListScreen(
            repository = repository,
            onBikeClick = { currentScreen = Screen.BikeDetail(it) },
            onSettingsClick = { currentScreen = Screen.Settings }
        )
        is Screen.BikeDetail -> BikeDetailScreen(
            bikeId = screen.bikeId,
            repository = repository,
            onBack = { currentScreen = Screen.BikeList }
        )
        is Screen.Settings -> SettingsScreen(
            repository = repository,
            onBack = { currentScreen = Screen.BikeList }
        )
    }
}
