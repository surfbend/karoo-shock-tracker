package com.shocktracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shocktracker.data.*
import com.shocktracker.repository.MaintenanceRepository
import com.shocktracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    bikeId: String,
    repository: MaintenanceRepository,
    onBack: () -> Unit
) {
    var record by remember { mutableStateOf(repository.getRecord(bikeId)) }
    var basicThreshold by remember { mutableStateOf(record?.basicServiceThreshold?.toString() ?: "50") }
    var fullThreshold by remember { mutableStateOf(record?.fullServiceThreshold?.toString() ?: "100") }
    var historicalHours by remember { mutableStateOf("") }
    var showServiceDialog by remember { mutableStateOf<Pair<ShockPosition, ServiceType>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(record?.bikeName ?: "Bike Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (record == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("Bike not found")
            }
            return@Scaffold
        }

        val currentRecord = record!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fork Status Card
            ServiceCard(
                title = "Fork",
                hoursSinceBasic = currentRecord.frontDescentHoursSinceBasic,
                hoursSinceFull = currentRecord.frontDescentHoursSinceFull,
                lastBasicDate = currentRecord.frontLastBasicServiceDate,
                lastFullDate = currentRecord.frontLastFullServiceDate,
                basicThreshold = currentRecord.basicServiceThreshold,
                fullThreshold = currentRecord.fullServiceThreshold,
                onBasicService = { showServiceDialog = ShockPosition.FRONT to ServiceType.BASIC },
                onFullService = { showServiceDialog = ShockPosition.FRONT to ServiceType.FULL }
            )

            // Rear Shock Status Card
            ServiceCard(
                title = "Rear Shock",
                hoursSinceBasic = currentRecord.rearDescentHoursSinceBasic,
                hoursSinceFull = currentRecord.rearDescentHoursSinceFull,
                lastBasicDate = currentRecord.rearLastBasicServiceDate,
                lastFullDate = currentRecord.rearLastFullServiceDate,
                basicThreshold = currentRecord.basicServiceThreshold,
                fullThreshold = currentRecord.fullServiceThreshold,
                onBasicService = { showServiceDialog = ShockPosition.REAR to ServiceType.BASIC },
                onFullService = { showServiceDialog = ShockPosition.REAR to ServiceType.FULL }
            )

            // Threshold Settings
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Service Thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = basicThreshold,
                        onValueChange = { basicThreshold = it },
                        label = { Text("Basic service (hours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fullThreshold,
                        onValueChange = { fullThreshold = it },
                        label = { Text("Full service (hours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val basic = basicThreshold.toDoubleOrNull() ?: 50.0
                            val full = fullThreshold.toDoubleOrNull() ?: 100.0
                            repository.updateThresholds(bikeId, basic, full)
                            record = repository.getRecord(bikeId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Thresholds")
                    }
                }
            }

            // Add Historical Hours
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Historical Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Add descent hours from past rides before using this app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = historicalHours,
                        onValueChange = { historicalHours = it },
                        label = { Text("Hours to add") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val hours = historicalHours.toDoubleOrNull()
                            if (hours != null && hours > 0) {
                                repository.addHistoricalHours(bikeId, hours)
                                record = repository.getRecord(bikeId)
                                historicalHours = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Hours")
                    }

                    if (currentRecord.historicalDescentHours > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Historical hours added: ${"%.1f".format(currentRecord.historicalDescentHours)}h",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Stats
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total rides tracked: ${currentRecord.totalRides}")
                    Text("Total descent hours: ${"%.1f".format(currentRecord.totalDescentHours)}h")
                    Text("Fork total hours: ${"%.1f".format(currentRecord.frontTotalDescentHours)}h")
                    Text("Rear total hours: ${"%.1f".format(currentRecord.rearTotalDescentHours)}h")
                }
            }
        }
    }

    // Service confirmation dialog
    showServiceDialog?.let { (position, type) ->
        AlertDialog(
            onDismissRequest = { showServiceDialog = null },
            title = { Text("Record Service?") },
            text = {
                val posName = if (position == ShockPosition.FRONT) "Fork" else "Rear shock"
                val typeName = if (type == ServiceType.BASIC) "basic" else "full"
                Text("Record $typeName service for $posName?\n\nThis will reset the service counter.")
            },
            confirmButton = {
                Button(onClick = {
                    repository.recordService(bikeId, position, type)
                    record = repository.getRecord(bikeId)
                    showServiceDialog = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServiceDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ServiceCard(
    title: String,
    hoursSinceBasic: Double,
    hoursSinceFull: Double,
    lastBasicDate: Long,
    lastFullDate: Long,
    basicThreshold: Double,
    fullThreshold: Double,
    onBasicService: () -> Unit,
    onFullService: () -> Unit
) {
    val basicPercent = (hoursSinceBasic / basicThreshold * 100)
    val fullPercent = (hoursSinceFull / fullThreshold * 100)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Basic service row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Basic Service", fontWeight = FontWeight.Medium)
                    Text(
                        "${"%.1f".format(hoursSinceBasic)}h / ${basicThreshold.toInt()}h (${"%.0f".format(basicPercent)}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Last: ${formatDate(lastBasicDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Button(onClick = onBasicService, colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full service row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Full Service", fontWeight = FontWeight.Medium)
                    Text(
                        "${"%.1f".format(hoursSinceFull)}h / ${fullThreshold.toInt()}h (${"%.0f".format(fullPercent)}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Last: ${formatDate(lastFullDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Button(onClick = onFullService, colors = ButtonDefaults.buttonColors(containerColor = StatusOrange)) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}
