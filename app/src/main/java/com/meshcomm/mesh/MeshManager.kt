package com.meshcomm.mesh

import android.content.Context
import android.util.Log
import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.model.MeshMessage
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.repository.MessageRepository
import com.meshcomm.location.LocationProvider
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Orchestrates mesh communication: Bluetooth transport, relay handler, and storage.
 */
class MeshManager(private val context: Context) {

    companion object {
        private const val TAG = "MeshManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val transportLayer = TransportLayer()
    private val repository = MessageRepository(AppDatabase.get(context))
    private val relayHandler = MessageRelayHandler(context, repository, transportLayer)
    private val bluetoothService = BluetoothService(context, transportLayer)
    private val locationProvider = LocationProvider(context)

    val incomingMessages: SharedFlow<com.meshcomm.data.model.Message> = relayHandler.incoming

    fun start() {
        locationProvider.startUpdates()
        bluetoothService.start()

        // Wire peer registry to relay handler
        PeerRegistry.registerMessageCallback { data, fromId ->
            Log.d(TAG, "Inbound raw data from $fromId (${data.take(32)}...)")
            relayHandler.onRaw(data, fromId)
        }

        Log.i(TAG, "MeshManager started: Bluetooth + relay active")
    }

    fun stop() {
        bluetoothService.stop()
        locationProvider.stopUpdates()
        relayHandler.clear()
        transportLayer.disconnectAll()
        PeerRegistry.clear()
        Log.i(TAG, "MeshManager stopped")
    }

    fun sendInfo(content: String, locationName: String? = null, ttl: Int = 5) {
        val (lat, lon) = locationProvider.getLastLatLon()
        val message = buildMeshMessage(
            messageType = MessageType.INFO,
            content = content,
            locationName = locationName,
            lat = lat,
            lon = lon,
            ttl = ttl
        )
        relayHandler.send(message)
    }

    fun sendSOS(content: String, ttl: Int = 8) {
        val (lat, lon) = locationProvider.getLastLatLon()
        val message = buildMeshMessage(
            messageType = MessageType.SOS,
            content = content,
            locationName = PrefsHelper.getLastLocationName(context).ifBlank { "Unknown location" },
            lat = lat,
            lon = lon,
            ttl = ttl
        )
        relayHandler.send(message)
    }

    fun storeLocationName(name: String) {
        PrefsHelper.setLastLocationName(context, name)
    }

    private fun buildMeshMessage(
        messageType: MessageType,
        content: String,
        locationName: String?,
        lat: Double,
        lon: Double,
        ttl: Int
    ): MeshMessage {
        val userId = PrefsHelper.getUserId(context)
        val name = PrefsHelper.getUserName(context)
        val resolvedLocationName = when {
            !locationName.isNullOrBlank() -> locationName
            PrefsHelper.getLastLocationName(context).isNotBlank() -> PrefsHelper.getLastLocationName(context)
            else -> "Unknown location"
        }
        if (resolvedLocationName.isNotBlank()) {
            PrefsHelper.setLastLocationName(context, resolvedLocationName)
        }
        return MeshMessage(
            messageId = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = System.currentTimeMillis(),
            latitude = lat,
            longitude = lon,
            locationName = resolvedLocationName,
            messageType = messageType,
            ttl = ttl,
            content = content,
            batteryLevel = BatteryHelper.getLevel(context),
            deviceId = userId
        )
    }
}
