package com.meshcomm.mesh

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstraction that holds outbound write channels for each connected peer.
 * BluetoothMeshManager and WiFiDirectManager register their sockets here.
 */
class TransportLayer {

    companion object {
        private const val TAG = "TransportLayer"
    }

    data class Channel(
        val peerId: String,
        val outputStream: OutputStream
    )

    private val channels = ConcurrentHashMap<String, Channel>()

    fun registerChannel(peerId: String, outputStream: OutputStream) {
        channels[peerId] = Channel(peerId, outputStream)
        Log.d(TAG, "Channel registered for $peerId (total: ${channels.size})")
    }

    fun unregisterChannel(peerId: String) {
        channels.remove(peerId)
        Log.d(TAG, "Channel unregistered for $peerId (remaining: ${channels.size})")
    }

    fun sendToAll(data: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        var successCount = 0
        channels.values.forEach { channel ->
            try {
                channel.outputStream.write(bytes)
                channel.outputStream.flush()
                successCount++
                Log.d(TAG, "Sent to ${channel.peerId}: ${data.take(50)}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send to ${channel.peerId}: ${e.message}")
                channels.remove(channel.peerId)
            }
        }
        Log.d(TAG, "Sent to $successCount/${channels.size} peers")
    }

    fun sendToAllExcept(data: String, excludeId: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        val targets = channels.values.filter { it.peerId != excludeId }
        var successCount = 0
        targets.forEach { channel ->
            try {
                channel.outputStream.write(bytes)
                channel.outputStream.flush()
                successCount++
                Log.d(TAG, "Sent to ${channel.peerId} (excluding $excludeId): ${data.take(50)}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send to ${channel.peerId}: ${e.message}")
                channels.remove(channel.peerId)
            }
        }
        Log.d(TAG, "Sent to $successCount/${targets.size} peers (excluded $excludeId)")
    }

    fun sendToPeer(peerId: String, data: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        try {
            channels[peerId]?.outputStream?.write(bytes)
            channels[peerId]?.outputStream?.flush()
            Log.d(TAG, "Sent to $peerId: ${data.take(50)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to $peerId: ${e.message}")
            channels.remove(peerId)
        }
    }

    fun getConnectedIds(): Set<String> = channels.keys.toSet()

    fun disconnectAll() {
        Log.d(TAG, "Disconnecting all ${channels.size} channels")
        channels.values.forEach { runCatching { it.outputStream.close() } }
        channels.clear()
    }
}
