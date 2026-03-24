package com.meshcomm.mesh

import android.content.Context
import android.util.Log
import com.google.gson.Gson
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
 * PRODUCTION-READY MESSAGE ROUTER
 * Handles multi-hop forwarding, deduplication, and battery-aware relaying.
 */
class MessageRouter(
    private val context: Context,
    private val repository: MessageRepository,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "MsgRouter"
        private const val MIN_BATTERY_FOR_RELAY = 15
    }

    private val selfId = PrefsHelper.getUserId(context)
    private val seenMessages = Collections.synchronizedSet(mutableSetOf<String>())
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    private val _incomingMessages = MutableSharedFlow<Message>(replay = 1)
    val incomingMessages: SharedFlow<Message> = _incomingMessages

    /** Called when raw JSON arrives from any transport */
    fun onRawDataReceived(json: String, fromPeerId: String) {
        scope.launch {
            try {
                val trimmedJson = json.trim()
                if (trimmedJson.isEmpty()) return@launch
                
                val message = gson.fromJson(trimmedJson, Message::class.java) ?: return@launch
                processMessage(message, fromPeerId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message from $fromPeerId: ${e.message}")
            }
        }
    }

    private suspend fun processMessage(message: Message, fromPeerId: String) {
        // 1. DEDUPLICATION (CRITICAL)
        if (seenMessages.contains(message.messageId)) return
        seenMessages.add(message.messageId)

        Log.i(TAG, "New message received: ${message.messageId} from $fromPeerId")

        // 2. TARGET CHECK & STORAGE
        val isForMe = message.targetId == null || message.targetId == selfId
        val isSOS = message.type == MessageType.SOS
        val isFromSelf = message.senderId == selfId

        if (!isFromSelf && (isForMe || isSOS)) {
            repository.saveMessage(message.copy(status = MessageStatus.DELIVERED))
            _incomingMessages.emit(message)
        } else if (!isFromSelf) {
            repository.saveMessage(message.copy(status = MessageStatus.RELAYED))
        }

        // 3. MULTI-HOP FLOODING (Forwarding)
        val battery = BatteryHelper.getLevel(context)
        val shouldForward = !isFromSelf && 
                          message.ttl > 0 && 
                          battery >= MIN_BATTERY_FOR_RELAY

        if (shouldForward) {
            val forwardedMsg = message.copy(ttl = message.ttl - 1)
            val json = gson.toJson(forwardedMsg)
            transportLayer.sendToAllExcept(json, fromPeerId)
            Log.d(TAG, "Forwarded message ${message.messageId}, TTL left: ${forwardedMsg.ttl}")
        }
    }

    /** Send a new message originating from this device */
    fun sendMessage(message: Message) {
        scope.launch {
            seenMessages.add(message.messageId)
            repository.saveMessage(message.copy(status = MessageStatus.SENT))
            
            val json = gson.toJson(message)
            transportLayer.sendToAll(json)
            
            // Only emit to local UI if it's NOT an SOS (SOS has special UI handling)
            if (message.type != MessageType.SOS) {
                _incomingMessages.emit(message)
            }
        }
    }
}
