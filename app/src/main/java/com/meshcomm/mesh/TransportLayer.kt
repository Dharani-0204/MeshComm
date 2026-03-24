package com.meshcomm.mesh

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstraction that holds outbound write channels for each connected peer.
 * Handles both Classic RFCOMM and BLE GATT write operations.
 */
class TransportLayer {

    companion object {
        private const val TAG = "TransportLayer"
    }

    private val channels = ConcurrentHashMap<String, java.io.OutputStream>()

    /**
     * Registers an output stream for a peer. 
     * For BLE, this is a custom OutputStream that triggers gatt.writeCharacteristic().
     */
    fun registerChannel(peerId: String, outputStream: java.io.OutputStream) {
        channels[peerId] = outputStream
        Log.d(TAG, "Channel registered for $peerId (total: ${channels.size})")
    }

    fun unregisterChannel(peerId: String) {
        channels.remove(peerId)
        Log.d(TAG, "Channel unregistered for $peerId (remaining: ${channels.size})")
    }

    fun sendToAll(data: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        channels.forEach { (id, stream) ->
            try {
                stream.write(bytes)
                stream.flush()
                Log.d(TAG, "Sent to $id: ${data.take(50)}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send to $id: ${e.message}")
                channels.remove(id)
            }
        }
    }

    fun sendToAllExcept(data: String, excludeId: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        channels.forEach { (id, stream) ->
            if (id != excludeId) {
                try {
                    stream.write(bytes)
                    stream.flush()
                    Log.d(TAG, "Forwarded to $id (excluding $excludeId)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to forward to $id: ${e.message}")
                    channels.remove(id)
                }
            }
        }
    }

    fun sendToPeer(peerId: String, data: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        try {
            channels[peerId]?.let { stream ->
                stream.write(bytes)
                stream.flush()
                Log.d(TAG, "Sent to $peerId: ${data.take(50)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to $peerId: ${e.message}")
            channels.remove(peerId)
        }
    }

    fun getConnectedIds(): Set<String> = channels.keys.toSet()

    fun disconnectAll() {
        channels.values.forEach { runCatching { it.close() } }
        channels.clear()
    }
}
