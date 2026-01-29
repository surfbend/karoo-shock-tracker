package com.shocktracker.data

import kotlinx.serialization.Serializable

@Serializable
enum class ServiceType {
    BASIC,  // Lower seals, oil change - every 50-100 descent hours
    FULL    // Complete rebuild - every 100-200 descent hours
}
