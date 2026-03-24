package com.meshcomm.mesh

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.meshcomm.crypto.EncryptionUtil
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageStatus
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.model.UserRole
import com.meshcomm.data.repository.MessageRepository
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.Collections

class MessageRouter(
    private val context: Context,
    private val repository: MessageRepository,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "MessageRouter"
    }

    private val selfId = PrefsHelper.getUserId(context)
    private val seenMessages: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    private val _incomingMessages = MutableSharedFlow<Message>(replay = 0)
    val incomingMessages: SharedFlow<Message> = _incomingMessages

    /** Called when raw JSON arrives from any transport */
    fun onRawDataReceived(json: String, fromPeerId: String) {
        scope.launch {
            try {
                val message = gson.fromJson(json, Message::class.java) ?: return@launch
                Log.d(TAG, "Received message from $fromPeerId: type=${message.type}, sender=${message.senderName}")
                processMessage(message, fromPeerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}", e)
            }
        }
    }

    private suspend fun processMessage(message: Message, fromPeerId: String) {
        // 1. Handshake handling
        if (message.type == MessageType.HANDSHAKE) {
            Log.i(TAG, "Handshake received from ${message.senderName} (${message.senderId})")
            val role = try { UserRole.valueOf(message.content) } catch (e: Exception) { UserRole.CIVILIAN }
            PeerRegistry.updatePeerInfo(
                fromPeerId, 
                message.senderId, 
                message.senderName, 
                role, 
                message.batteryLevel,
                message.latitude,
                message.longitude
            )
            return
        }

        // 2. Deduplication
        if (seenMessages.contains(message.messageId)) {
            Log.d(TAG, "Duplicate message ignored: ${message.messageId}")
            return
        }
        seenMessages.add(message.messageId)
        repository.markSeen(message.messageId)

        // 3. Update peer location if present in any message
        if (message.latitude != 0.0 && message.longitude != 0.0) {
            PeerRegistry.updatePeerLocation(
                message.senderId, 
                message.latitude, 
                message.longitude,
                message.type == MessageType.SOS
            )
        }

        // 4. Decrypt if needed
        val decrypted = EncryptionUtil.decryptMessage(message)

        // 5. Determine if this device is the intended recipient
        val isForMe = decrypted.targetId == null || decrypted.targetId == selfId
        val isSOS   = decrypted.type == MessageType.SOS
        val isSelf  = decrypted.senderId == selfId

        // 6. Store and emit if relevant to this user (but NOT if sender is self)
        if ((isForMe || isSOS) && !isSelf) {
            val stored = decrypted.copy(status = MessageStatus.DELIVERED)
            repository.saveMessage(stored)
            _incomingMessages.emit(stored)
            Log.i(TAG, "Message delivered: type=${decrypted.type}, from=${decrypted.senderName}")
        } else if (isSelf) {
            Log.d(TAG, "Ignoring own message echo: ${message.messageId}")
        } else {
            // Relayed messages
            repository.saveMessage(decrypted.copy(status = MessageStatus.RELAYED))
        }

        // 7. Forwarding
        val battery = BatteryHelper.getLevel(context)
        val shouldRelay = battery > 30 && decrypted.ttl > 0 && decrypted.senderId != selfId

        if (shouldRelay) {
            val forwarded = decrypted.copy(ttl = decrypted.ttl - 1, status = MessageStatus.RELAYED)
            transportLayer.sendToAllExcept(gson.toJson(forwarded), fromPeerId)
        }
    }

    /** Send a new message originating from this device */
    fun sendMessage(message: Message) {
        scope.launch {
            val prepared = if (message.type == MessageType.HANDSHAKE) message else EncryptionUtil.encryptMessage(message)
            if (message.type != MessageType.HANDSHAKE) {
                seenMessages.add(prepared.messageId)
                repository.saveMessage(prepared.copy(status = MessageStatus.SENT))
            }
            
            val json = gson.toJson(prepared)
            transportLayer.sendToAll(json)

            if (prepared.type != MessageType.SOS && prepared.type != MessageType.HANDSHAKE) {
                _incomingMessages.emit(prepared)
            }
        }
    }

    fun sendHandshake() {
        val deviceId = PrefsHelper.getUserId(context)
        val location = (context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager)
            ?.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)

        val handshake = Message(
            senderId = deviceId,
            senderName = PrefsHelper.getUserName(context),
            type = MessageType.HANDSHAKE,
            content = PrefsHelper.getUserRole(context).name,
            batteryLevel = BatteryHelper.getLevel(context),
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0,
            deviceId = deviceId
        )
        sendMessage(handshake)
    }
}
