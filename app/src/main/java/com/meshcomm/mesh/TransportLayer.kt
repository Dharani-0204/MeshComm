package com.meshcomm.mesh

import android.util.Log
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced TransportLayer that supports both Stream-based (WiFi/Classic BT)
 * and Packet-based (BLE GATT) communication.
 */
class TransportLayer {

    companion object {
        private const val TAG = "TransportLayer"
    }

    interface Sender {
        fun send(data: String)
        fun close()
    }

    private val senders = ConcurrentHashMap<String, Sender>()

    fun registerSender(peerId: String, sender: Sender) {
        senders[peerId] = sender
        Log.d(TAG, "Sender registered for $peerId (total: ${senders.size})")
    }

    fun unregisterSender(peerId: String) {
        senders.remove(peerId)?.close()
        Log.d(TAG, "Sender unregistered for $peerId (remaining: ${senders.size})")
    }

    /** Helper for legacy OutputStream registration */
    fun registerChannel(peerId: String, outputStream: OutputStream) {
        registerSender(peerId, object : Sender {
            override fun send(data: String) {
                try {
                    outputStream.write((data + "\n").toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Stream write failed for $peerId: ${e.message}")
                    unregisterSender(peerId)
                }
            }
            override fun close() {
                runCatching { outputStream.close() }
            }
        })
    }

    fun sendToAll(data: String) {
        senders.forEach { (id, sender) ->
            sender.send(data)
        }
        Log.d(TAG, "Broadcast to ${senders.size} peers")
    }

    fun sendToAllExcept(data: String, excludeId: String) {
        senders.forEach { (id, sender) ->
            if (id != excludeId) {
                sender.send(data)
            }
        }
    }

    fun sendToPeer(peerId: String, data: String) {
        senders[peerId]?.send(data) ?: Log.w(TAG, "No sender found for $peerId")
    }

    fun getConnectedIds(): Set<String> = senders.keys.toSet()

    fun disconnectAll() {
        Log.d(TAG, "Disconnecting all ${senders.size} senders")
        senders.values.forEach { it.close() }
        senders.clear()
    }
}
