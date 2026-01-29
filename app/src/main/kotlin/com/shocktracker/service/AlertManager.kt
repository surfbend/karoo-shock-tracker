package com.shocktracker.service

import android.graphics.Color
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.InRideAlert
import com.shocktracker.R
import com.shocktracker.data.*
import com.shocktracker.repository.MaintenanceRepository
import timber.log.Timber

class AlertManager(
    private val karooSystem: KarooSystemService,
    private val repository: MaintenanceRepository
) {

    fun shouldAlert(record: ShockMaintenanceRecord): Boolean {
        val config = repository.getThresholdConfig()
        if (!config.alertEnabled) return false

        return record.isBasicServiceDue(ShockPosition.FRONT) ||
                record.isBasicServiceDue(ShockPosition.REAR) ||
                record.isFullServiceDue(ShockPosition.FRONT) ||
                record.isFullServiceDue(ShockPosition.REAR)
    }

    fun showMaintenanceAlert(record: ShockMaintenanceRecord) {
        val alerts = mutableListOf<String>()

        // Check each shock and service type
        if (record.isFullServiceDue(ShockPosition.FRONT)) {
            alerts.add("Fork FULL service due (${"%.1f".format(record.frontDescentHoursSinceFull)}h)")
        } else if (record.isBasicServiceDue(ShockPosition.FRONT)) {
            alerts.add("Fork basic service due (${"%.1f".format(record.frontDescentHoursSinceBasic)}h)")
        }

        if (record.isFullServiceDue(ShockPosition.REAR)) {
            alerts.add("Rear FULL service due (${"%.1f".format(record.rearDescentHoursSinceFull)}h)")
        } else if (record.isBasicServiceDue(ShockPosition.REAR)) {
            alerts.add("Rear basic service due (${"%.1f".format(record.rearDescentHoursSinceBasic)}h)")
        }

        if (alerts.isEmpty()) return

        val detail = "${record.bikeName}:\n${alerts.joinToString("\n")}"

        try {
            karooSystem.dispatch(
                InRideAlert(
                    id = "shock-service-alert-${record.bikeId}",
                    title = "Suspension Service Due",
                    detail = detail,
                    icon = R.drawable.ic_shock,
                    autoDismissMs = 15000,
                    backgroundColor = Color.parseColor("#FF5722"),
                    textColor = Color.WHITE
                )
            )
            Timber.i("Maintenance alert shown for bike: ${record.bikeName}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show maintenance alert")
        }
    }

    fun showStatusAlert(record: ShockMaintenanceRecord) {
        val frontBasicRemaining = record.basicServiceThreshold - record.frontDescentHoursSinceBasic
        val rearBasicRemaining = record.basicServiceThreshold - record.rearDescentHoursSinceBasic

        val detail = buildString {
            append(record.bikeName)
            append("\n\nFork: ${"%.1f".format(record.frontDescentHoursSinceBasic)}h")
            if (frontBasicRemaining > 0) {
                append(" (${"%.1f".format(frontBasicRemaining)}h to service)")
            } else {
                append(" (SERVICE DUE)")
            }
            append("\n\nRear: ${"%.1f".format(record.rearDescentHoursSinceBasic)}h")
            if (rearBasicRemaining > 0) {
                append(" (${"%.1f".format(rearBasicRemaining)}h to service)")
            } else {
                append(" (SERVICE DUE)")
            }
        }

        try {
            karooSystem.dispatch(
                InRideAlert(
                    id = "shock-status-${record.bikeId}",
                    title = "Suspension Status",
                    detail = detail,
                    icon = R.drawable.ic_shock,
                    autoDismissMs = 10000,
                    backgroundColor = Color.parseColor("#2196F3"),
                    textColor = Color.WHITE
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to show status alert")
        }
    }
}
