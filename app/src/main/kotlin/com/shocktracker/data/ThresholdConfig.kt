package com.shocktracker.data

import kotlinx.serialization.Serializable

/**
 * Global configuration for shock service thresholds.
 */
@Serializable
data class ThresholdConfig(
    // Default service intervals (descent hours)
    val defaultBasicServiceHours: Double = 50.0,
    val defaultFullServiceHours: Double = 100.0,

    // Alert preferences
    val alertEnabled: Boolean = true,
    val alertAtRideEnd: Boolean = true,

    // Warning thresholds (percentage of service interval)
    val warningThreshold: Double = 0.8,  // 80% - show yellow
    val criticalThreshold: Double = 1.0  // 100% - show red
)
