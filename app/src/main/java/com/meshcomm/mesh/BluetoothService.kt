package com.meshcomm.mesh

import android.content.Context
import android.util.Log
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Enhanced Bluetooth service for robust mesh networking with adaptive discovery,
 * connection quality monitoring, and battery optimization.
 */
class BluetoothService(
    private val context: Context,
    private val transportLayer: TransportLayer
) {

    companion object {
        private const val TAG = "BluetoothService"

        // Discovery timing (milliseconds)
        private const val MIN_DISCOVERY_INTERVAL = 15_000L  // 15 seconds minimum
        private const val MAX_DISCOVERY_INTERVAL = 120_000L // 2 minutes maximum
        private const val DEFAULT_DISCOVERY_INTERVAL = 45_000L // 45 seconds default

        // Network health thresholds
        private const val MIN_HEALTHY_PEERS = 2
        private const val MAX_OPTIMAL_PEERS = 8

        // Battery conservation levels
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val bluetoothMeshManager = BluetoothMeshManager(context, transportLayer)

    private var discoveryJob: Job? = null
    private var healthMonitorJob: Job? = null

    // Network state tracking
    private var currentDiscoveryInterval = DEFAULT_DISCOVERY_INTERVAL
    private var lastSuccessfulDiscovery = 0L
    private var consecutiveFailures = 0
    private var isNetworkHealthy = false

    // Statistics
    private var totalDiscoveries = 0
    private var successfulConnections = 0
    private var messagesRelayed = 0

    fun start() {
        Log.i(TAG, "Starting enhanced Bluetooth mesh service")

        bluetoothMeshManager.startServer()
        bluetoothMeshManager.startAdvertising()

        startAdaptiveDiscoveryLoop()
        startNetworkHealthMonitoring()

        Log.i(TAG, "Bluetooth service started with adaptive discovery")
    }

    private fun startAdaptiveDiscoveryLoop() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            while (isActive) {
                val batteryLevel = BatteryHelper.getLevel(context)
                val canOperate = shouldOperateBasedOnBattery(batteryLevel)

                if (canOperate) {
                    performDiscovery()
                    updateDiscoveryInterval()
                } else {
                    Log.d(TAG, "Skipping discovery due to battery conservation (${batteryLevel}%)")
                }

                delay(currentDiscoveryInterval)
            }
        }
    }

    private fun startNetworkHealthMonitoring() {
        healthMonitorJob?.cancel()
        healthMonitorJob = scope.launch {
            while (isActive) {
                updateNetworkHealth()
                logNetworkStats()
                delay(30_000) // Monitor every 30 seconds
            }
        }
    }

    private suspend fun performDiscovery() {
        val startTime = System.currentTimeMillis()
        totalDiscoveries++

        try {
            bluetoothMeshManager.startDiscovery()

            // Wait a bit to see if we find peers
            delay(5000)

            val connectedPeers = transportLayer.getConnectedIds().size

            if (connectedPeers > 0) {
                lastSuccessfulDiscovery = System.currentTimeMillis()
                consecutiveFailures = 0
                successfulConnections++
                Log.d(TAG, "Discovery successful: found $connectedPeers peers")
            } else {
                consecutiveFailures++
                Log.d(TAG, "Discovery yielded no new connections (attempt ${consecutiveFailures})")
            }

        } catch (e: Exception) {
            consecutiveFailures++
            Log.w(TAG, "Discovery failed: ${e.message}")
        }

        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Discovery cycle completed in ${duration}ms")
    }

    private fun updateDiscoveryInterval() {
        val connectedPeers = transportLayer.getConnectedIds().size
        val batteryLevel = BatteryHelper.getLevel(context)

        // Base interval adjustment based on network density
        val densityMultiplier = when {
            connectedPeers >= MAX_OPTIMAL_PEERS -> 1.5 // Slow down when well-connected
            connectedPeers >= MIN_HEALTHY_PEERS -> 1.0 // Normal speed
            connectedPeers == 0 -> 0.7 // Speed up when isolated
            else -> 0.8
        }

        // Battery conservation multiplier
        val batteryMultiplier = when {
            batteryLevel <= CRITICAL_BATTERY_THRESHOLD -> 3.0 // Very slow
            batteryLevel <= LOW_BATTERY_THRESHOLD -> 2.0 // Slow
            else -> 1.0
        }

        // Failure recovery adjustment
        val failureMultiplier = when {
            consecutiveFailures > 5 -> 1.5 // Back off on repeated failures
            consecutiveFailures > 2 -> 1.2
            else -> 1.0
        }

        val newInterval = (DEFAULT_DISCOVERY_INTERVAL * densityMultiplier * batteryMultiplier * failureMultiplier).toLong()
        currentDiscoveryInterval = max(MIN_DISCOVERY_INTERVAL, min(MAX_DISCOVERY_INTERVAL, newInterval))

        Log.d(TAG, "Discovery interval adjusted to ${currentDiscoveryInterval / 1000}s (peers=$connectedPeers, battery=$batteryLevel%, failures=$consecutiveFailures)")
    }

    private fun updateNetworkHealth() {
        val connectedPeers = transportLayer.getConnectedIds().size
        val timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessfulDiscovery

        isNetworkHealthy = connectedPeers >= MIN_HEALTHY_PEERS || timeSinceLastSuccess < 120_000 // 2 minutes

        // Store network metrics for other components
        PrefsHelper.setNetworkPeerCount(context, connectedPeers)
        PrefsHelper.setNetworkHealthy(context, isNetworkHealthy)
    }

    private fun shouldOperateBasedOnBattery(batteryLevel: Int): Boolean {
        return when {
            batteryLevel <= CRITICAL_BATTERY_THRESHOLD -> false // Completely stop at critical battery
            batteryLevel <= LOW_BATTERY_THRESHOLD -> {
                // At low battery, operate only occasionally
                System.currentTimeMillis() % 3 == 0L // Roughly 1/3 of the time
            }
            else -> true
        }
    }

    private fun logNetworkStats() {
        val connectedPeers = transportLayer.getConnectedIds().size
        val successRate = if (totalDiscoveries > 0) {
            (successfulConnections * 100) / totalDiscoveries
        } else 0

        Log.i(TAG, "Network Stats: peers=$connectedPeers, discoveries=$totalDiscoveries, " +
                "success=${successRate}%, healthy=$isNetworkHealthy, interval=${currentDiscoveryInterval/1000}s")
    }

    fun onMessageRelayed() {
        messagesRelayed++
    }

    fun getNetworkStats(): NetworkStats {
        return NetworkStats(
            connectedPeers = transportLayer.getConnectedIds().size,
            totalDiscoveries = totalDiscoveries,
            successfulConnections = successfulConnections,
            messagesRelayed = messagesRelayed,
            isHealthy = isNetworkHealthy,
            discoveryInterval = currentDiscoveryInterval
        )
    }

    fun stop() {
        Log.i(TAG, "Stopping Bluetooth service")

        discoveryJob?.cancel()
        healthMonitorJob?.cancel()
        bluetoothMeshManager.stopAll()

        logNetworkStats()
        Log.i(TAG, "Bluetooth service stopped")
    }

    fun makeDiscoverable(activity: android.app.Activity) {
        bluetoothMeshManager.makeDiscoverable(activity)
    }

    fun forceDiscovery() {
        scope.launch {
            Log.d(TAG, "Forcing immediate discovery")
            performDiscovery()
        }
    }
}

/**
 * Network statistics data class
 */
data class NetworkStats(
    val connectedPeers: Int,
    val totalDiscoveries: Int,
    val successfulConnections: Int,
    val messagesRelayed: Int,
    val isHealthy: Boolean,
    val discoveryInterval: Long
)
