package com.shocktracker.data

import kotlinx.serialization.Serializable

/**
 * Tracks metrics for an in-progress ride.
 * Focuses on descent time which stresses suspension.
 */
@Serializable
data class RideSession(
    val bikeId: String,
    val bikeName: String,
    val startTime: Long = System.currentTimeMillis(),

    // Descent tracking
    var totalDescentMeters: Double = 0.0,  // Total elevation lost
    var descentTimeMs: Long = 0L,          // Time spent descending
    var elapsedTimeMs: Long = 0L,          // Total ride time

    // For calculating descent (need previous altitude)
    var lastAltitude: Double? = null,
    var isDescending: Boolean = false,
    var descentStartTime: Long = 0L
) {
    /**
     * Get descent time in hours.
     */
    fun getDescentHours(): Double = descentTimeMs / 3600000.0

    /**
     * Get total elapsed time in hours.
     */
    fun getElapsedHours(): Double = elapsedTimeMs / 3600000.0

    /**
     * Get descent percentage of ride.
     */
    fun getDescentPercentage(): Double {
        if (elapsedTimeMs == 0L) return 0.0
        return (descentTimeMs.toDouble() / elapsedTimeMs) * 100
    }
}
