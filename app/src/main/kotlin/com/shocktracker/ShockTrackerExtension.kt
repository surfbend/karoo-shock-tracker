package com.shocktracker

import android.graphics.Color
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.*
import com.shocktracker.data.RideSession
import com.shocktracker.data.ShockPosition
import com.shocktracker.data.ServiceType
import com.shocktracker.repository.MaintenanceRepository
import com.shocktracker.service.AlertManager
import kotlinx.coroutines.*
import timber.log.Timber

class ShockTrackerExtension : KarooExtension("shock-tracker", "1.0.0") {

    companion object {
        // Data type ID for elevation loss (descent) from Karoo
        private const val DATA_TYPE_ELEVATION_LOSS = "TYPE_ELEVATION_LOSS_0"
    }

    private lateinit var karooSystem: KarooSystemService
    private lateinit var repository: MaintenanceRepository
    private lateinit var alertManager: AlertManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val consumerIds = mutableListOf<String>()
    private var descentConsumerId: String? = null

    private var currentProfileId: String? = null
    private var currentBikeId: String? = null
    private var currentBikeName: String? = null
    private var activeSession: RideSession? = null
    private var isRecording: Boolean = false
    private var knownBikes: List<Bikes.Bike> = emptyList()

    // Track descent data from Karoo
    private var currentDescentMeters: Double = 0.0
    private var hasReceivedDescentData: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("ShockTrackerExtension onCreate")

        karooSystem = KarooSystemService(this)
        repository = MaintenanceRepository(this)
        alertManager = AlertManager(karooSystem, repository)

        karooSystem.connect { connected ->
            if (connected) {
                Timber.d("Connected to Karoo system")
                registerEventConsumers()
                checkForActiveSession()
            }
        }
    }

    override fun onDestroy() {
        Timber.d("ShockTrackerExtension onDestroy")
        cleanupConsumers()
        karooSystem.disconnect()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBonusAction(actionId: String) {
        Timber.d("Bonus action: $actionId")
        when (actionId) {
            "service-front-basic" -> handleServiceAction(ShockPosition.FRONT, ServiceType.BASIC)
            "service-rear-basic" -> handleServiceAction(ShockPosition.REAR, ServiceType.BASIC)
            "check-status" -> handleCheckStatusAction()
        }
    }

    private fun registerEventConsumers() {
        consumerIds += karooSystem.addConsumer { bikes: Bikes ->
            handleBikesUpdate(bikes.bikes)
        }

        consumerIds += karooSystem.addConsumer { event: ActiveRideProfile ->
            handleProfileChange(event.profile)
        }

        consumerIds += karooSystem.addConsumer { rideState: RideState ->
            handleRideStateChange(rideState)
        }

        Timber.d("Registered ${consumerIds.size} event consumers")
    }

    private fun cleanupConsumers() {
        consumerIds.forEach { karooSystem.removeConsumer(it) }
        consumerIds.clear()
    }

    private fun handleBikesUpdate(bikes: List<Bikes.Bike>) {
        Timber.d("Received ${bikes.size} bikes")
        knownBikes = bikes
        bikes.forEach { bike ->
            repository.ensureBikeRecord(bike.id, bike.name)
        }
    }

    private fun handleProfileChange(profile: RideProfile) {
        Timber.d("Active profile: ${profile.id} - ${profile.name}")
        currentProfileId = profile.id

        val mappedBikeId = repository.getBikeIdForProfile(profile.id)
        if (mappedBikeId != null) {
            currentBikeId = mappedBikeId
            currentBikeName = knownBikes.find { it.id == mappedBikeId }?.name
            return
        }

        val matchingBike = knownBikes.find { bike ->
            profile.name.contains(bike.name, ignoreCase = true) ||
                    bike.name.contains(profile.name, ignoreCase = true)
        }

        if (matchingBike != null) {
            currentBikeId = matchingBike.id
            currentBikeName = matchingBike.name
        } else if (knownBikes.size == 1) {
            currentBikeId = knownBikes.first().id
            currentBikeName = knownBikes.first().name
        } else {
            currentBikeId = profile.id
            currentBikeName = profile.name
            repository.ensureBikeRecord(profile.id, profile.name)
        }
    }

    private fun handleRideStateChange(rideState: RideState) {
        when (rideState) {
            is RideState.Recording -> {
                if (!isRecording) {
                    Timber.i("Ride started")
                    isRecording = true
                    startRideTracking()
                }
            }
            is RideState.Paused -> {
                Timber.d("Ride paused")
            }
            is RideState.Idle -> {
                if (isRecording) {
                    Timber.i("Ride ended")
                    isRecording = false
                    finalizeRide()
                }
            }
        }
    }

    private fun startRideTracking() {
        val bikeId = currentBikeId ?: return
        val bikeName = currentBikeName ?: "Unknown"

        activeSession = RideSession(bikeId = bikeId, bikeName = bikeName)
        repository.saveActiveSession(activeSession)
        currentDescentMeters = 0.0
        hasReceivedDescentData = false

        // Start listening for descent data from Karoo
        startDescentConsumer()

        Timber.i("Started tracking ride for: $bikeName")
    }

    private fun startDescentConsumer() {
        // Try to consume OnStreamState for elevation loss data
        try {
            descentConsumerId = karooSystem.addConsumer(
                OnStreamState.StartStreaming(dataTypeId = DATA_TYPE_ELEVATION_LOSS)
            ) { streamState: OnStreamState ->
                when (val state = streamState.state) {
                    is StreamState.Streaming -> {
                        state.dataPoint?.singleValue?.let { value ->
                            currentDescentMeters = value
                            hasReceivedDescentData = true
                            activeSession?.totalDescentMeters = value
                            Timber.d("Descent updated: ${"%.1f".format(value)}m")
                        }
                    }
                    is StreamState.NotAvailable -> {
                        Timber.d("Descent data not available")
                    }
                    is StreamState.Searching -> {
                        Timber.d("Searching for descent data...")
                    }
                    else -> {
                        Timber.d("Stream state: $state")
                    }
                }
            }
            Timber.d("Started descent consumer: $descentConsumerId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start descent consumer, will use estimate")
        }
    }

    private fun stopDescentConsumer() {
        descentConsumerId?.let { id ->
            karooSystem.removeConsumer(id)
            descentConsumerId = null
            Timber.d("Stopped descent consumer")
        }
    }

    private fun finalizeRide() {
        val session = activeSession ?: return
        stopDescentConsumer()

        // Calculate elapsed time for fallback estimation
        val elapsedHours = (System.currentTimeMillis() - session.startTime) / 3600000.0

        // Use actual descent data from Karoo if available, otherwise estimate
        val descentMeters: Double
        val usedActualData: Boolean

        if (hasReceivedDescentData && currentDescentMeters > 0) {
            descentMeters = currentDescentMeters
            usedActualData = true
        } else if (session.totalDescentMeters > 0) {
            descentMeters = session.totalDescentMeters
            usedActualData = true
        } else {
            // Fallback: estimate descent as ~30% of ride time converted to meters
            // Using the descent rate: if descent rate is 300m/hr, then in 1 hour of descending
            // we cover 300m. If we estimate 30% of ride is descending, then:
            // descentMeters = elapsedHours * 0.30 * descentRate
            val descentRate = repository.getDescentRate()
            descentMeters = elapsedHours * 0.30 * descentRate
            usedActualData = false
            Timber.w("Using estimated descent (no actual data available)")
        }

        // Convert descent meters to descent hours
        val averageDescentRateMetersPerHour = repository.getDescentRate()
        val descentHours = descentMeters / averageDescentRateMetersPerHour

        if (descentHours < 0.01 || descentMeters < 10) {
            Timber.d("Ride has minimal descent (${descentMeters}m), not counting")
            clearActiveSession()
            return
        }

        repository.addRideDescentHours(session.bikeId, descentHours)

        val record = repository.getRecord(session.bikeId)
        if (record != null && alertManager.shouldAlert(record)) {
            alertManager.showMaintenanceAlert(record)
        }

        val dataSource = if (usedActualData) "actual" else "estimated"
        Timber.i("Finalized ride: ${"%.0f".format(descentMeters)}m descended ($dataSource) = ${"%.2f".format(descentHours)}h")
        clearActiveSession()
    }

    private fun clearActiveSession() {
        activeSession = null
        currentDescentMeters = 0.0
        hasReceivedDescentData = false
        repository.saveActiveSession(null)
    }

    private fun checkForActiveSession() {
        val savedSession = repository.getActiveSession()
        if (savedSession != null) {
            Timber.i("Recovered active session for: ${savedSession.bikeName}")
            activeSession = savedSession
            currentBikeId = savedSession.bikeId
            currentBikeName = savedSession.bikeName
            isRecording = true
        }
    }

    private fun handleServiceAction(position: ShockPosition, serviceType: ServiceType) {
        val bikeId = currentBikeId ?: return

        repository.recordService(bikeId, position, serviceType)

        val positionName = if (position == ShockPosition.FRONT) "Fork" else "Rear shock"
        val typeName = if (serviceType == ServiceType.BASIC) "basic" else "full"

        try {
            karooSystem.dispatch(
                InRideAlert(
                    id = "shock-service-confirmed",
                    title = "Service Recorded!",
                    detail = "$positionName $typeName service recorded.\nHours reset.",
                    icon = R.drawable.ic_shock,
                    autoDismissMs = 5000,
                    backgroundColor = Color.parseColor("#4CAF50"),
                    textColor = Color.WHITE
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to show confirmation")
        }
    }

    private fun handleCheckStatusAction() {
        val bikeId = currentBikeId ?: return
        val record = repository.getRecord(bikeId)
        if (record != null) {
            alertManager.showStatusAlert(record)
        }
    }
}
