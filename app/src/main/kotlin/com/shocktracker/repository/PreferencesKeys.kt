package com.shocktracker.repository

object PreferencesKeys {
    const val PREFS_NAME = "shock_tracker_prefs"
    const val KEY_THRESHOLD_CONFIG = "threshold_config"
    const val KEY_MAINTENANCE_RECORDS = "maintenance_records"
    const val KEY_SERVICE_HISTORY = "service_history"
    const val KEY_ACTIVE_SESSION = "active_ride_session"
    const val KEY_BIKE_PROFILE_MAP = "bike_profile_map"
    const val KEY_DESCENT_RATE = "descent_rate_meters_per_hour"

    // Default descent rate: 300 meters per hour
    // This represents typical MTB descent including technical sections, rest stops, etc.
    const val DEFAULT_DESCENT_RATE = 300.0
}
