package com.shocktracker.data

import kotlinx.serialization.Serializable

@Serializable
enum class ShockPosition {
    FRONT,  // Fork
    REAR    // Rear shock
}
