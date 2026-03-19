package com.meshcomm.mesh

import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstraction that holds outbound write channels for each connected peer.
 * BluetoothMeshManager and WiFiDirectManager register their sockets here.
 */
class TransportLayer {

    data class Channel(
        val peerId: String,
        val outputStream: OutputStream
    )

    private val channels = ConcurrentHashMap<String, Channel>()

    fun registerChannel(peerId: String, outputStream: OutputStream) {
        channels[peerId] = Channel(peerId, outputStream)
    }

    fun unregisterChannel(peerId: String) {
        channels.remove(peerId)
    }

    fun sendToAll(data: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        channels.values.forEach { channel ->
            try { channel.outputStream.write(bytes) }
            catch (e: Exception) { channels.remove(channel.peerId) }
        }
    }

    fun sendToAllExcept(data: String, excludeId: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        channels.values.filter { it.peerId != excludeId }.forEach { channel ->
            try { channel.outputStream.write(bytes) }
            catch (e: Exception) { channels.remove(channel.peerId) }
        }
    }

    fun sendToPeer(peerId: String, data: String) {
        val bytes = (data + "\n").toByteArray(Charsets.UTF_8)
        try { channels[peerId]?.outputStream?.write(bytes) }
        catch (e: Exception) { channels.remove(peerId) }
    }

    fun getConnectedIds(): Set<String> = channels.keys.toSet()

    fun disconnectAll() {
        channels.values.forEach { runCatching { it.outputStream.close() } }
        channels.clear()
    }
}
