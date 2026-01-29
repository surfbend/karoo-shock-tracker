package com.shocktracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shocktracker.repository.MaintenanceRepository
import com.shocktracker.ui.components.ShockStatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeListScreen(
    repository: MaintenanceRepository,
    onBikeClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var records by remember { mutableStateOf(repository.getAllRecords()) }
    val config by remember { mutableStateOf(repository.getThresholdConfig()) }

    LaunchedEffect(Unit) {
        while (true) {
            records = repository.getAllRecords()
            kotlinx.coroutines.delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shock Tracker") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("No Bikes Found", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Add bikes in Karoo settings.\nThey will appear here automatically.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records, key = { it.bikeId }) { record ->
                    ShockStatusCard(
                        record = record,
                        config = config,
                        onClick = { onBikeClick(record.bikeId) }
                    )
                }
            }
        }
    }
}
