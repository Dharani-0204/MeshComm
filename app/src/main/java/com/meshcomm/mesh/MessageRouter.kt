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
        private const val TAG = "MessageRouter"
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
                val message = gson.fromJson(json, Message::class.java) ?: return@launch
                Log.d(TAG, "Received message from $fromPeerId: type=${message.type}, sender=${message.senderName}")
                processMessage(message, fromPeerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}", e)
            }
        }
    }

    private suspend fun processMessage(message: Message, fromPeerId: String) {
        // 1. DEDUPLICATION (CRITICAL)
        if (seenMessages.contains(message.messageId)) return
        // 1. Deduplication
        if (seenMessages.contains(message.messageId)) {
            Log.d(TAG, "Duplicate message ignored: ${message.messageId}")
            return
        }
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
        // 3. Determine if this device is the intended recipient
        val isForMe = decrypted.targetId == null || decrypted.targetId == selfId
        val isSOS   = decrypted.type == MessageType.SOS
        val isSelf  = decrypted.senderId == selfId

        // 4. Store and emit if relevant to this user (but NOT if sender is self)
        if ((isForMe || isSOS) && !isSelf) {
            val stored = decrypted.copy(status = MessageStatus.DELIVERED)
            repository.saveMessage(stored)
            _incomingMessages.emit(stored)
            Log.i(TAG, "Message delivered to user: type=${decrypted.type}, from=${decrypted.senderName}")
        } else if (isSelf) {
            Log.d(TAG, "Ignoring own message echo: ${message.messageId}")
        } else {
            // Store relayed messages too (for audit)
            repository.saveMessage(decrypted.copy(status = MessageStatus.RELAYED))
            Log.d(TAG, "Message stored as relayed: ${message.messageId}")
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
        if (shouldRelay) {
            val connectedDevices = transportLayer.getConnectedIds().size
            val forwarded = decrypted.copy(
                ttl = decrypted.ttl - 1,
                status = MessageStatus.RELAYED
            )
            // Do NOT re-encrypt — encryption was done by original sender
            val json = gson.toJson(forwarded)
            transportLayer.sendToAllExcept(json, excludeId = fromPeerId)
            Log.i(TAG, "Message forwarded to $connectedDevices devices (excluding $fromPeerId): ${message.messageId}")
        } else {
            Log.d(TAG, "Message NOT forwarded: battery=$battery%, ttl=${decrypted.ttl}, isSelf=$isSelf")
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
            val prepared = EncryptionUtil.encryptMessage(message)
            seenMessages.add(prepared.messageId)  // Don't process own echoes
            repository.saveMessage(prepared.copy(status = MessageStatus.SENT))
            val json = gson.toJson(prepared)

            val connectedDevices = transportLayer.getConnectedIds().size
            transportLayer.sendToAll(json)

            Log.i(TAG, "Message sent to $connectedDevices devices: type=${prepared.type}, id=${prepared.messageId}")

            // For SOS messages, DO NOT emit to sender's own UI (no self-notification)
            // For other messages, show in own UI immediately
            if (prepared.type != MessageType.SOS) {
                _incomingMessages.emit(prepared)
                Log.d(TAG, "Message emitted to own UI: ${prepared.messageId}")
            } else {
                Log.d(TAG, "SOS NOT emitted to sender UI (prevents self-notification): ${prepared.messageId}")
            }
        }
    }
}
