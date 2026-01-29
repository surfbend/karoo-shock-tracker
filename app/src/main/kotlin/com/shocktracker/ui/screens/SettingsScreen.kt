package com.shocktracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shocktracker.data.ThresholdConfig
import com.shocktracker.repository.MaintenanceRepository
import com.shocktracker.repository.PreferencesKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: MaintenanceRepository,
    onBack: () -> Unit
) {
    var config by remember { mutableStateOf(repository.getThresholdConfig()) }
    var basicHours by remember { mutableStateOf(config.defaultBasicServiceHours.toString()) }
    var fullHours by remember { mutableStateOf(config.defaultFullServiceHours.toString()) }
    var alertEnabled by remember { mutableStateOf(config.alertEnabled) }
    var descentRate by remember { mutableStateOf(repository.getDescentRate().toInt().toString()) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Default Service Intervals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Used when bike-specific thresholds are not set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = basicHours,
                        onValueChange = { basicHours = it },
                        label = { Text("Basic service interval (hours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Lower seals, oil change. Typically 50-100 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fullHours,
                        onValueChange = { fullHours = it },
                        label = { Text("Full service interval (hours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Complete rebuild. Typically 100-200 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Show alerts at ride end")
                        Switch(checked = alertEnabled, onCheckedChange = { alertEnabled = it })
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Descent Calculation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descentRate,
                        onValueChange = { descentRate = it },
                        label = { Text("Descent rate (m/hour)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Converts descent meters to equivalent descent hours. " +
                        "Lower = more conservative (200-250 for aggressive DH). " +
                        "Higher = less conservative (350-450 for flow trails). " +
                        "Default: 300 m/hr for technical trail riding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How It Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This app uses actual descent data from your Karoo to track suspension wear. " +
                        "Each ride's total descent (meters dropped) is converted to equivalent " +
                        "descent hours using the rate above. This is more accurate than time-based " +
                        "estimates since suspension stress correlates with vertical drop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Button(
                onClick = {
                    val newConfig = ThresholdConfig(
                        defaultBasicServiceHours = basicHours.toDoubleOrNull() ?: 50.0,
                        defaultFullServiceHours = fullHours.toDoubleOrNull() ?: 100.0,
                        alertEnabled = alertEnabled
                    )
                    repository.saveThresholdConfig(newConfig)
                    repository.setDescentRate(descentRate.toDoubleOrNull() ?: PreferencesKeys.DEFAULT_DESCENT_RATE)
                    config = newConfig
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }

            if (saved) {
                Text("Settings saved!", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
