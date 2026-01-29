package com.shocktracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shocktracker.data.ShockMaintenanceRecord
import com.shocktracker.data.ShockPosition
import com.shocktracker.data.ThresholdConfig
import com.shocktracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShockStatusCard(
    record: ShockMaintenanceRecord,
    config: ThresholdConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = record.bikeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fork status
            ShockRow(
                label = "Fork",
                hours = record.frontDescentHoursSinceBasic,
                threshold = record.basicServiceThreshold,
                lastService = record.frontLastBasicServiceDate
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rear shock status
            ShockRow(
                label = "Rear",
                hours = record.rearDescentHoursSinceBasic,
                threshold = record.basicServiceThreshold,
                lastService = record.rearLastBasicServiceDate
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total rides: ${record.totalRides} | Total descent: ${"%.1f".format(record.totalDescentHours)}h",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ShockRow(
    label: String,
    hours: Double,
    threshold: Double,
    lastService: Long
) {
    val percentage = (hours / threshold * 100).coerceIn(0.0, 150.0)
    val statusColor = when {
        percentage >= 100 -> StatusRed
        percentage >= 80 -> StatusOrange
        percentage >= 60 -> StatusYellow
        else -> StatusGreen
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Surface(color = statusColor, shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = "${"%.1f".format(hours)}h / ${threshold.toInt()}h",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LinearProgressIndicator(
            progress = (percentage / 100).toFloat().coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = "Last service: ${formatDate(lastService)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}
