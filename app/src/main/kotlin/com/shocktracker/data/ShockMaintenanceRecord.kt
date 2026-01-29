package com.shocktracker.data

import kotlinx.serialization.Serializable

/**
 * Maintenance tracking record for a bike's suspension.
 * Tracks front fork and rear shock separately.
 */
@Serializable
data class ShockMaintenanceRecord(
    val bikeId: String,
    val bikeName: String,

    // Front Fork tracking
    val frontLastBasicServiceDate: Long = 0L,
    val frontLastFullServiceDate: Long = 0L,
    val frontDescentHoursSinceBasic: Double = 0.0,
    val frontDescentHoursSinceFull: Double = 0.0,
    val frontTotalDescentHours: Double = 0.0,

    // Rear Shock tracking
    val rearLastBasicServiceDate: Long = 0L,
    val rearLastFullServiceDate: Long = 0L,
    val rearDescentHoursSinceBasic: Double = 0.0,
    val rearDescentHoursSinceFull: Double = 0.0,
    val rearTotalDescentHours: Double = 0.0,

    // Ride stats
    val totalRides: Int = 0,
    val totalDescentHours: Double = 0.0,

    // Threshold settings (hours)
    val basicServiceThreshold: Double = 50.0,  // Default 50 hours
    val fullServiceThreshold: Double = 100.0,  // Default 100 hours

    // Historical hours (manually entered from past rides)
    val historicalDescentHours: Double = 0.0
) {
    /**
     * Get descent hours since last basic service for a shock position.
     */
    fun getHoursSinceBasic(position: ShockPosition): Double {
        return when (position) {
            ShockPosition.FRONT -> frontDescentHoursSinceBasic
            ShockPosition.REAR -> rearDescentHoursSinceBasic
        }
    }

    /**
     * Get descent hours since last full service for a shock position.
     */
    fun getHoursSinceFull(position: ShockPosition): Double {
        return when (position) {
            ShockPosition.FRONT -> frontDescentHoursSinceFull
            ShockPosition.REAR -> rearDescentHoursSinceFull
        }
    }

    /**
     * Check if basic service is due for a shock position.
     */
    fun isBasicServiceDue(position: ShockPosition): Boolean {
        return getHoursSinceBasic(position) >= basicServiceThreshold
    }

    /**
     * Check if full service is due for a shock position.
     */
    fun isFullServiceDue(position: ShockPosition): Boolean {
        return getHoursSinceFull(position) >= fullServiceThreshold
    }

    /**
     * Get the most urgent service needed.
     */
    fun getMostUrgentService(): Pair<ShockPosition, ServiceType>? {
        val checks = listOf(
            Triple(ShockPosition.FRONT, ServiceType.FULL, getHoursSinceFull(ShockPosition.FRONT) / fullServiceThreshold),
            Triple(ShockPosition.REAR, ServiceType.FULL, getHoursSinceFull(ShockPosition.REAR) / fullServiceThreshold),
            Triple(ShockPosition.FRONT, ServiceType.BASIC, getHoursSinceBasic(ShockPosition.FRONT) / basicServiceThreshold),
            Triple(ShockPosition.REAR, ServiceType.BASIC, getHoursSinceBasic(ShockPosition.REAR) / basicServiceThreshold)
        )

        val mostUrgent = checks.filter { it.third >= 1.0 }.maxByOrNull { it.third }
        return mostUrgent?.let { it.first to it.second }
    }

    /**
     * Get status summary string.
     */
    fun getStatusSummary(): String {
        return "Front: ${"%.1f".format(frontDescentHoursSinceBasic)}h | Rear: ${"%.1f".format(rearDescentHoursSinceBasic)}h"
    }
}
