package com.meshcomm.mesh

import android.content.Context
import com.google.gson.Gson
import com.meshcomm.crypto.EncryptionUtil
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

class MessageRouter(
    private val context: Context,
    private val repository: MessageRepository,
    private val transportLayer: TransportLayer
) {
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
                processMessage(message, fromPeerId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun processMessage(message: Message, fromPeerId: String) {
        // 1. Deduplication
        if (seenMessages.contains(message.messageId)) return
        seenMessages.add(message.messageId)
        repository.markSeen(message.messageId)

        // 2. Decrypt if needed
        val decrypted = EncryptionUtil.decryptMessage(message)

        // 3. Determine if this device is the intended recipient
        val isForMe = decrypted.targetId == null || decrypted.targetId == selfId
        val isSOS   = decrypted.type == MessageType.SOS

        // 4. Store and emit if relevant to this user
        if (isForMe || isSOS) {
            val stored = decrypted.copy(status = MessageStatus.DELIVERED)
            repository.saveMessage(stored)
            _incomingMessages.emit(stored)
        } else {
            // Store relayed messages too (for audit)
            repository.saveMessage(decrypted.copy(status = MessageStatus.RELAYED))
        }

        // 5. Forward decision
        val battery = BatteryHelper.getLevel(context)
        val shouldRelay = battery > 30
                       && decrypted.ttl > 0
                       && decrypted.senderId != selfId

        if (shouldRelay) {
            val forwarded = decrypted.copy(
                ttl = decrypted.ttl - 1,
                status = MessageStatus.RELAYED
            )
            // Do NOT re-encrypt — encryption was done by original sender
            val json = gson.toJson(forwarded)
            transportLayer.sendToAllExcept(json, excludeId = fromPeerId)
        }
    }

    /** Send a new message originating from this device */
    fun sendMessage(message: Message) {
        scope.launch {
            val prepared = EncryptionUtil.encryptMessage(message)
            seenMessages.add(prepared.messageId)  // Don't process own echoes
            repository.saveMessage(prepared.copy(status = MessageStatus.SENT))
            val json = gson.toJson(prepared)
            transportLayer.sendToAll(json)
            _incomingMessages.emit(prepared)      // Show in own UI immediately
        }
    }

    fun getSeenCount(): Int = seenMessages.size
}
