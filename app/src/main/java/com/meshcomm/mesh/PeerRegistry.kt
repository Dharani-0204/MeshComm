package com.meshcomm.mesh

import com.meshcomm.data.model.PeerDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry for mesh peers.
 * Supports partial updates to preserve GATT connection state.
 */
object PeerRegistry {
    private val peers = ConcurrentHashMap<String, PeerDevice>()
    private val _peerFlow = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peerFlow: StateFlow<List<PeerDevice>> = _peerFlow

    private val messageCallbacks = mutableListOf<(String, String) -> Unit>()

    fun addPeer(peer: PeerDevice) {
        val existing = peers[peer.deviceId]
        // Update if it's new or metadata changed
        if (existing == null || existing.deviceName != peer.deviceName || 
            existing.batteryLevel != peer.batteryLevel || existing.rssi != peer.rssi) {
            
            peers[peer.deviceId] = peer.copy(
                isConnected = existing?.isConnected ?: false // Preserve connection state
            )
            notifyObservers()
        }
    }

    fun updateConnectionStatus(deviceId: String, isConnected: Boolean) {
        val existing = peers[deviceId] ?: return
        if (existing.isConnected != isConnected) {
            peers[deviceId] = existing.copy(isConnected = isConnected)
            notifyObservers()
        }
    }

    fun removePeer(deviceId: String) {
        if (peers.remove(deviceId) != null) {
            notifyObservers()
        }
    }

    private fun notifyObservers() {
        // Sort by RSSI so nearest users are at the top
        _peerFlow.value = peers.values.toList().sortedByDescending { it.rssi }
    }

    fun getConnectedCount(): Int = peers.values.count { it.isConnected }

    fun registerMessageCallback(cb: (data: String, fromId: String) -> Unit) {
        messageCallbacks.add(cb)
    }

    fun dispatchIncoming(data: String, fromId: String) {
        messageCallbacks.forEach { it(data, fromId) }
    }

    fun getConnectedPeers(): List<PeerDevice> = peers.values.filter { it.isConnected }.toList()

    fun clear() {
        peers.clear()
        _peerFlow.value = emptyList()
    }
}
