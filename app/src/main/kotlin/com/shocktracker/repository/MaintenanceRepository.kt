package com.shocktracker.repository

import android.content.Context
import android.content.SharedPreferences
import com.shocktracker.data.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

class MaintenanceRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PreferencesKeys.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ========== Maintenance Records ==========

    fun getAllRecords(): List<ShockMaintenanceRecord> {
        val jsonStr = prefs.getString(PreferencesKeys.KEY_MAINTENANCE_RECORDS, null)
        return try {
            jsonStr?.let { json.decodeFromString<List<ShockMaintenanceRecord>>(it) } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode maintenance records")
            emptyList()
        }
    }

    fun getRecord(bikeId: String): ShockMaintenanceRecord? {
        return getAllRecords().find { it.bikeId == bikeId }
    }

    fun saveRecord(record: ShockMaintenanceRecord) {
        val records = getAllRecords().toMutableList()
        val index = records.indexOfFirst { it.bikeId == record.bikeId }
        if (index >= 0) {
            records[index] = record
        } else {
            records.add(record)
        }
        prefs.edit()
            .putString(PreferencesKeys.KEY_MAINTENANCE_RECORDS, json.encodeToString(records))
            .apply()
        Timber.d("Saved record for bike: ${record.bikeName}")
    }

    fun ensureBikeRecord(bikeId: String, bikeName: String) {
        if (getRecord(bikeId) == null) {
            saveRecord(ShockMaintenanceRecord(bikeId = bikeId, bikeName = bikeName))
            Timber.d("Created new record for bike: $bikeName")
        }
    }

    /**
     * Record a service performed on a shock.
     */
    fun recordService(
        bikeId: String,
        position: ShockPosition,
        serviceType: ServiceType,
        serviceDate: Long = System.currentTimeMillis(),
        notes: String = ""
    ) {
        val record = getRecord(bikeId) ?: return

        val updated = when (position) {
            ShockPosition.FRONT -> when (serviceType) {
                ServiceType.BASIC -> record.copy(
                    frontLastBasicServiceDate = serviceDate,
                    frontDescentHoursSinceBasic = 0.0
                )
                ServiceType.FULL -> record.copy(
                    frontLastFullServiceDate = serviceDate,
                    frontDescentHoursSinceFull = 0.0,
                    frontLastBasicServiceDate = serviceDate,
                    frontDescentHoursSinceBasic = 0.0
                )
            }
            ShockPosition.REAR -> when (serviceType) {
                ServiceType.BASIC -> record.copy(
                    rearLastBasicServiceDate = serviceDate,
                    rearDescentHoursSinceBasic = 0.0
                )
                ServiceType.FULL -> record.copy(
                    rearLastFullServiceDate = serviceDate,
                    rearDescentHoursSinceFull = 0.0,
                    rearLastBasicServiceDate = serviceDate,
                    rearDescentHoursSinceBasic = 0.0
                )
            }
        }

        saveRecord(updated)

        // Also save to service history
        val serviceRecord = ServiceRecord(
            id = UUID.randomUUID().toString(),
            bikeId = bikeId,
            shockPosition = position,
            serviceType = serviceType,
            serviceDate = serviceDate,
            descentHoursAtService = when (position) {
                ShockPosition.FRONT -> record.frontTotalDescentHours
                ShockPosition.REAR -> record.rearTotalDescentHours
            },
            notes = notes
        )
        addServiceHistory(serviceRecord)

        Timber.i("Recorded $serviceType service for ${position.name} on ${record.bikeName}")
    }

    /**
     * Add descent hours from a completed ride.
     */
    fun addRideDescentHours(bikeId: String, descentHours: Double) {
        val record = getRecord(bikeId) ?: return
        val updated = record.copy(
            frontDescentHoursSinceBasic = record.frontDescentHoursSinceBasic + descentHours,
            frontDescentHoursSinceFull = record.frontDescentHoursSinceFull + descentHours,
            frontTotalDescentHours = record.frontTotalDescentHours + descentHours,
            rearDescentHoursSinceBasic = record.rearDescentHoursSinceBasic + descentHours,
            rearDescentHoursSinceFull = record.rearDescentHoursSinceFull + descentHours,
            rearTotalDescentHours = record.rearTotalDescentHours + descentHours,
            totalRides = record.totalRides + 1,
            totalDescentHours = record.totalDescentHours + descentHours
        )
        saveRecord(updated)
        Timber.i("Added ${"%.2f".format(descentHours)}h descent to ${record.bikeName}")
    }

    /**
     * Add historical descent hours (from past rides before using the app).
     */
    fun addHistoricalHours(bikeId: String, hours: Double) {
        val record = getRecord(bikeId) ?: return
        val updated = record.copy(
            historicalDescentHours = record.historicalDescentHours + hours,
            frontDescentHoursSinceBasic = record.frontDescentHoursSinceBasic + hours,
            frontDescentHoursSinceFull = record.frontDescentHoursSinceFull + hours,
            rearDescentHoursSinceBasic = record.rearDescentHoursSinceBasic + hours,
            rearDescentHoursSinceFull = record.rearDescentHoursSinceFull + hours
        )
        saveRecord(updated)
        Timber.i("Added ${"%.1f".format(hours)}h historical hours to ${record.bikeName}")
    }

    /**
     * Update service thresholds for a bike.
     */
    fun updateThresholds(bikeId: String, basicHours: Double, fullHours: Double) {
        val record = getRecord(bikeId) ?: return
        val updated = record.copy(
            basicServiceThreshold = basicHours,
            fullServiceThreshold = fullHours
        )
        saveRecord(updated)
    }

    // ========== Service History ==========

    fun getServiceHistory(bikeId: String? = null): List<ServiceRecord> {
        val jsonStr = prefs.getString(PreferencesKeys.KEY_SERVICE_HISTORY, null)
        val all = try {
            jsonStr?.let { json.decodeFromString<List<ServiceRecord>>(it) } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode service history")
            emptyList()
        }
        return if (bikeId != null) all.filter { it.bikeId == bikeId } else all
    }

    private fun addServiceHistory(record: ServiceRecord) {
        val history = getServiceHistory().toMutableList()
        history.add(record)
        prefs.edit()
            .putString(PreferencesKeys.KEY_SERVICE_HISTORY, json.encodeToString(history))
            .apply()
    }

    // ========== Threshold Config ==========

    fun getThresholdConfig(): ThresholdConfig {
        val jsonStr = prefs.getString(PreferencesKeys.KEY_THRESHOLD_CONFIG, null)
        return try {
            jsonStr?.let { json.decodeFromString<ThresholdConfig>(it) } ?: ThresholdConfig()
        } catch (e: Exception) {
            ThresholdConfig()
        }
    }

    fun saveThresholdConfig(config: ThresholdConfig) {
        prefs.edit()
            .putString(PreferencesKeys.KEY_THRESHOLD_CONFIG, json.encodeToString(config))
            .apply()
    }

    // ========== Active Session ==========

    fun getActiveSession(): RideSession? {
        val jsonStr = prefs.getString(PreferencesKeys.KEY_ACTIVE_SESSION, null)
        return try {
            jsonStr?.let { json.decodeFromString<RideSession>(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun saveActiveSession(session: RideSession?) {
        if (session == null) {
            prefs.edit().remove(PreferencesKeys.KEY_ACTIVE_SESSION).apply()
        } else {
            prefs.edit()
                .putString(PreferencesKeys.KEY_ACTIVE_SESSION, json.encodeToString(session))
                .apply()
        }
    }

    // ========== Bike-Profile Mapping ==========

    fun getBikeProfileMap(): Map<String, String> {
        val jsonStr = prefs.getString(PreferencesKeys.KEY_BIKE_PROFILE_MAP, null)
        return try {
            jsonStr?.let { json.decodeFromString<Map<String, String>>(it) } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getBikeIdForProfile(profileId: String): String? {
        return getBikeProfileMap()[profileId]
    }

    // ========== Descent Rate Settings ==========

    /**
     * Get the descent rate used to convert meters descended to descent hours.
     * Default is 300 m/hour (typical MTB descent including technical terrain).
     */
    fun getDescentRate(): Double {
        return prefs.getFloat(
            PreferencesKeys.KEY_DESCENT_RATE,
            PreferencesKeys.DEFAULT_DESCENT_RATE.toFloat()
        ).toDouble()
    }

    /**
     * Set the descent rate (meters per hour).
     * Lower values = more aggressive (faster descents = more stress per meter)
     * Higher values = more conservative (slower descents = less stress per meter)
     *
     * Suggested ranges:
     * - Aggressive DH/enduro: 200-250 m/hr
     * - Technical trail: 250-350 m/hr
     * - Flow trails: 350-450 m/hr
     */
    fun setDescentRate(metersPerHour: Double) {
        prefs.edit()
            .putFloat(PreferencesKeys.KEY_DESCENT_RATE, metersPerHour.toFloat())
            .apply()
        Timber.d("Set descent rate to ${"%.0f".format(metersPerHour)} m/hr")
    }
}
