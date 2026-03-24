package com.meshcomm.mesh

import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

object PeerRegistry {
    private const val TAG = "PeerRegistry"
    private val peers = ConcurrentHashMap<String, PeerDevice>()
    private val _peerFlow = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peerFlow: StateFlow<List<PeerDevice>> = _peerFlow

    // Map to track transport ID to actual Device ID (after handshake)
    private val transportIdToDeviceId = ConcurrentHashMap<String, String>()

    // Callbacks from transport layers
    private val messageCallbacks = mutableListOf<(String, String) -> Unit>()

    fun addPeer(peer: PeerDevice) {
        peers[peer.deviceId] = peer.copy(isConnected = true)
        updateFlow()
        Log.d(TAG, "Peer added: ${peer.deviceId} (${peer.deviceName})")
    }

    fun updatePeerInfo(transportId: String, deviceId: String, name: String, role: UserRole, battery: Int, lat: Double = 0.0, lon: Double = 0.0) {
        transportIdToDeviceId[transportId] = deviceId
        val existing = peers[deviceId] ?: peers[transportId]
        
        val updatedPeer = PeerDevice(
            deviceId = deviceId,
            deviceName = name,
            transport = existing?.transport ?: peers.values.find { it.deviceId == transportId }?.transport ?: return,
            role = role,
            batteryLevel = battery,
            isConnected = true,
            latitude = if (lat != 0.0) lat else existing?.latitude ?: 0.0,
            longitude = if (lon != 0.0) lon else existing?.longitude ?: 0.0,
            lastSeen = System.currentTimeMillis()
        )
        
        // Remove old entry if transport ID was different from device ID
        if (transportId != deviceId) {
            peers.remove(transportId)
        }
        
        peers[deviceId] = updatedPeer
        updateFlow()
        Log.i(TAG, "Peer updated via Handshake: $name ($deviceId), Role: $role")
    }

    fun updatePeerLocation(deviceId: String, lat: Double, lon: Double, isSos: Boolean = false) {
        val actualId = transportIdToDeviceId[deviceId] ?: deviceId
        peers[actualId]?.let {
            it.latitude = lat
            it.longitude = lon
            it.isSosActive = isSos
            it.lastSeen = System.currentTimeMillis()
            updateFlow()
        }
    }

    fun removePeer(transportId: String) {
        val deviceId = transportIdToDeviceId[transportId] ?: transportId
        peers[deviceId]?.let {
            peers[deviceId] = it.copy(isConnected = false)
        }
        transportIdToDeviceId.remove(transportId)
        updateFlow()
        Log.d(TAG, "Peer disconnected: $transportId")
    }

    private fun updateFlow() {
        _peerFlow.value = peers.values.filter { it.isConnected }.toList()
    }

    fun getConnectedPeers(): List<PeerDevice> = _peerFlow.value

    fun getConnectedCount(): Int = _peerFlow.value.size

    fun registerMessageCallback(cb: (data: String, fromId: String) -> Unit) {
        messageCallbacks.add(cb)
    }

    fun dispatchIncoming(data: String, fromId: String) {
        messageCallbacks.forEach { it(data, fromId) }
    }

    fun clear() {
        peers.clear()
        transportIdToDeviceId.clear()
        _peerFlow.value = emptyList()
    }
}
