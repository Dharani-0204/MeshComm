package com.meshcomm.mesh

import com.meshcomm.data.model.PeerDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

object PeerRegistry {
    private val peers = ConcurrentHashMap<String, PeerDevice>()
    private val _peerFlow = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peerFlow: StateFlow<List<PeerDevice>> = _peerFlow

    // Callbacks from transport layers
    private val messageCallbacks = mutableListOf<(String, String) -> Unit>()

    fun addPeer(peer: PeerDevice) {
        peers[peer.deviceId] = peer.copy(isConnected = true)
        _peerFlow.value = peers.values.filter { it.isConnected }.toList()
    }

    fun removePeer(deviceId: String) {
        peers[deviceId] = peers[deviceId]?.copy(isConnected = false) ?: return
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
        _peerFlow.value = emptyList()
    }
}
