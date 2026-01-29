package com.shocktracker.data

import kotlinx.serialization.Serializable

/**
 * Record of a service event for a shock.
 */
@Serializable
data class ServiceRecord(
    val id: String,
    val bikeId: String,
    val shockPosition: ShockPosition,
    val serviceType: ServiceType,
    val serviceDate: Long,  // Unix timestamp ms
    val descentHoursAtService: Double,  // Total descent hours when serviced
    val notes: String = ""
)
