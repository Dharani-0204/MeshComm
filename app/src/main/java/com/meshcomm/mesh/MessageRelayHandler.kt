package com.meshcomm.mesh

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.meshcomm.data.model.MeshMessage
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageStatus
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.repository.MessageRepository
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Mesh-aware relay handler that performs TTL-based forwarding, deduplication,
 * and persistence for all Bluetooth mesh messages.
 */
class MessageRelayHandler(
    private val context: Context,
    private val repository: MessageRepository,
    private val transportLayer: TransportLayer
    ) {

    companion object {
        private const val TAG = "MessageRelayHandler"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val seenMessages: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val selfId = PrefsHelper.getUserId(context)

    private val _incoming = MutableSharedFlow<Message>(replay = 0)
    val incoming: SharedFlow<Message> = _incoming

    fun onRaw(json: String, fromPeerId: String) {
        scope.launch {
            runCatching {
                val meshMessage = gson.fromJson(json, MeshMessage::class.java)
                meshMessage?.let { handleIncoming(it, fromPeerId) }
            }.onFailure { Log.e(TAG, "Failed to parse incoming data: ${it.message}", it) }
        }
    }

    private suspend fun handleIncoming(meshMessage: MeshMessage, fromPeerId: String) {
        if (seenMessages.contains(meshMessage.messageId)) {
            Log.d(TAG, "Duplicate message ignored: ${meshMessage.messageId}")
            return
        }
        seenMessages.add(meshMessage.messageId)
        repository.markSeen(meshMessage.messageId)

        // Drop expired messages early
        if (meshMessage.ttl <= 0) {
            Log.d(TAG, "TTL exhausted, dropping message ${meshMessage.messageId}")
            return
        }

        val delivered = meshMessage.toDomainMessage()
        val isSelf = delivered.senderId == selfId

        // Persist and emit for UI when not self
        if (!isSelf) {
            repository.saveMessage(delivered.copy(status = MessageStatus.DELIVERED))
            _incoming.emit(delivered.copy(status = MessageStatus.DELIVERED))
            Log.d(TAG, "Message delivered from ${delivered.senderId} (${delivered.messageId})")
        }

        // Forward with TTL decrement if we are allowed to relay
        val canRelay = BatteryHelper.canRelay(context) && !isSelf && meshMessage.ttl > 0
        if (canRelay) {
            val forwarded = meshMessage.copy(ttl = meshMessage.ttl - 1)
            val json = gson.toJson(forwarded)
            transportLayer.sendToAllExcept(json, fromPeerId)
            Log.d(TAG, "Forwarded message ${meshMessage.messageId} with TTL ${forwarded.ttl}")
        } else {
            Log.d(TAG, "Skipping relay for ${meshMessage.messageId}: canRelay=$canRelay")
        }
    }

    fun send(meshMessage: MeshMessage) {
        scope.launch {
            val prepared = meshMessage.copy(
                messageId = meshMessage.messageId.ifBlank { java.util.UUID.randomUUID().toString() }
            )
            seenMessages.add(prepared.messageId) // avoid echo

            val domain = prepared.toDomainMessage()
            repository.saveMessage(domain.copy(status = MessageStatus.SENT))

            val json = gson.toJson(prepared)
            transportLayer.sendToAll(json)
            Log.d(TAG, "Sent mesh message ${prepared.messageId} to ${transportLayer.getConnectedIds().size} peers")

            // Emit locally for INFO messages so sender UI updates instantly
            if (prepared.messageType != MessageType.SOS) {
                _incoming.emit(domain)
            }
        }
    }

    fun clear() {
        seenMessages.clear()
    }

    private fun MeshMessage.toDomainMessage(): Message {
        val senderName = if (userId == selfId) {
            PrefsHelper.getUserName(context)
        } else {
            userId
        }
        return Message(
            messageId = messageId,
            senderId = userId,
            senderName = senderName,
            targetId = null,
            type = messageType,
            content = content,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            batteryLevel = batteryLevel,
            nearbyDevicesCount = transportLayer.getConnectedIds().size,
            timestamp = timestamp,
            ttl = ttl,
            status = MessageStatus.DELIVERED,
            deviceId = userId
        )
    }
}
