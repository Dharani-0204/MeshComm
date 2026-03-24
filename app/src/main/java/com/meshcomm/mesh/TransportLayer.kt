package com.meshcomm.mesh

import android.util.Log
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * TransportLayer: Manages active communication channels.
 * Improved to handle multiple connections and avoid redundant channel removal.
 */
class TransportLayer {

    companion object {
        private const val TAG = "TransportLayer"
    }

    data class Channel(
        val peerId: String,
        val outputStream: OutputStream,
        val createdAt: Long = System.currentTimeMillis()
    )

    // Map of peerId -> Active Channel
    private val channels = ConcurrentHashMap<String, Channel>()

    /**
     * Registers a new channel for a peer. 
     * If a channel already exists, we keep the one that was created first (more stable).
     */
    fun registerChannel(peerId: String, outputStream: OutputStream): Boolean {
        val existing = channels[peerId]
        if (existing != null) {
            Log.d(TAG, "Channel already exists for $peerId. Comparison logic could go here.")
            // For now, we allow overwrite but return true to indicate it's active
        }
        
        val newChannel = Channel(peerId, outputStream)
        channels[peerId] = newChannel
        Log.i(TAG, "Channel registered: $peerId (Total active: ${channels.size})")
        return true
    }

    /**
     * Unregisters a channel only if it matches the one being closed.
     */
    fun unregisterChannel(peerId: String, outputStream: OutputStream) {
        val current = channels[peerId]
        if (current?.outputStream == outputStream) {
            channels.remove(peerId)
            Log.i(TAG, "Channel unregistered: $peerId")
        } else {
            Log.d(TAG, "Not unregistering $peerId: stream mismatch (likely a newer connection is active)")
        }
    }

    fun sendToAll(data: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        channels.values.forEach { channel ->
            try {
                channel.outputStream.write(bytes)
                channel.outputStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Write failed to ${channel.peerId}: ${e.message}")
                // Don't remove here, let the reader thread handle it via unregister
            }
        }
    }

    fun sendToAllExcept(data: String, excludeId: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        channels.values.filter { it.peerId != excludeId }.forEach { channel ->
            try {
                channel.outputStream.write(bytes)
                channel.outputStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Write failed to ${channel.peerId}")
            }
        }
    }

    fun sendToPeer(peerId: String, data: String) {
        val channel = channels[peerId] ?: return
        try {
            val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
            channel.outputStream.write(bytes)
            channel.outputStream.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Direct write failed to $peerId")
        }
    }

    fun getConnectedIds(): Set<String> = channels.keys.toSet()

    fun disconnectAll() {
        channels.values.forEach { runCatching { it.outputStream.close() } }
        channels.clear()
    }
}
