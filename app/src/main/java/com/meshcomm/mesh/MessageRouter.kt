package com.meshcomm.mesh

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.meshcomm.crypto.EncryptionUtil
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageStatus
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.repository.MessageRepository
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class MessageRouter(
    private val context: Context,
    private val repository: MessageRepository,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "MessageRouter"
        private const val MAX_RETRIES = 3
        private const val SOS_MAX_RETRIES = 5 // Higher priority for SOS
        private const val INITIAL_BACKOFF = 1000L
        private const val CHUNK_ACK_TIMEOUT = 5000L
    }

    private val selfId = PrefsHelper.getUserId(context)
    private val seenMessages: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private val chunkingManager = ChunkingManager(context)

    private val chunkAcks = ConcurrentHashMap<String, Boolean>()

    private val _incomingMessages = MutableSharedFlow<Message>(replay = 0)
    val incomingMessages: SharedFlow<Message> = _incomingMessages

    fun onRawDataReceived(json: String, fromPeerId: String) {
        scope.launch {
            try {
                val message = gson.fromJson(json, Message::class.java) ?: return@launch
                
                if (message.type == MessageType.CHUNK_ACK) {
                    val key = "${message.originalMessageId}_${message.chunkIndex}"
                    chunkAcks[key] = true
                    return@launch
                }

                processMessage(message, fromPeerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing raw data: ${e.message}")
            }
        }
    }

    private suspend fun processMessage(message: Message, fromPeerId: String) {
        if (seenMessages.contains(message.messageId)) {
            return
        }
        seenMessages.add(message.messageId)
        
        if (message.type != MessageType.CHUNK) {
            repository.markSeen(message.messageId)
        }

        if (message.type == MessageType.CHUNK) {
            val reassembled = chunkingManager.processChunk(message) { ack ->
                transportLayer.sendToAll(gson.toJson(ack))
            }
            
            if (reassembled != null) {
                processMessage(reassembled, fromPeerId)
            }
            
            forwardIfNeeded(message, fromPeerId)
            return
        }

        val decrypted = EncryptionUtil.decryptMessage(message)

        val isForMe = decrypted.targetId == null || decrypted.targetId == selfId
        val isSOS   = decrypted.type == MessageType.SOS
        val isMedia = decrypted.type == MessageType.AUDIO || decrypted.type == MessageType.IMAGE
        val isSelf  = decrypted.senderId == selfId

        if ((isForMe || isSOS || isMedia) && !isSelf) {
            val stored = decrypted.copy(status = MessageStatus.DELIVERED)
            repository.saveMessage(stored)
            _incomingMessages.emit(stored)
            Log.i(TAG, "Message delivered: type=${decrypted.type}, from=${decrypted.senderName}")
        } else if (!isSelf) {
            repository.saveMessage(decrypted.copy(status = MessageStatus.RELAYED))
        }

        forwardIfNeeded(decrypted, fromPeerId)
    }

    private fun forwardIfNeeded(message: Message, fromPeerId: String) {
        val battery = BatteryHelper.getLevel(context)
        val isSOS = message.type == MessageType.SOS
        
        // SOS messages bypass battery constraints for maximum reach
        // Other messages are only relayed if battery > 15% (Battery Optimized)
        val shouldRelay = (isSOS || battery > 15) && message.ttl > 0 && message.senderId != selfId

        if (shouldRelay) {
            val forwarded = message.copy(
                ttl = message.ttl - 1,
                status = MessageStatus.RELAYED
            )
            val json = gson.toJson(forwarded)
            transportLayer.sendToAllExcept(json, excludeId = fromPeerId)
            Log.d(TAG, "Relayed ${message.type} from ${message.senderName}, battery=$battery%")
        }
    }

    fun sendMessage(message: Message) {
        scope.launch {
            if (message.type == MessageType.AUDIO || message.type == MessageType.IMAGE) {
                repository.saveMessage(message.copy(status = MessageStatus.SENT))
                _incomingMessages.emit(message)

                val chunks = chunkingManager.chunkify(message)
                chunks.forEach { chunk ->
                    sendChunkReliably(chunk)
                }
                return@launch
            }

            val prepared = EncryptionUtil.encryptMessage(message)
            seenMessages.add(prepared.messageId)
            repository.saveMessage(prepared.copy(status = MessageStatus.SENT))
            
            sendWithRetry(prepared)

            if (prepared.type != MessageType.SOS) {
                _incomingMessages.emit(prepared)
            }
        }
    }

    private suspend fun sendChunkReliably(chunk: Message) {
        val key = "${chunk.originalMessageId}_${chunk.chunkIndex}"
        val json = gson.toJson(chunk)
        var attempt = 0
        var acknowledged = false

        while (attempt < MAX_RETRIES && !acknowledged) {
            chunkAcks[key] = false
            transportLayer.sendToAll(json)
            
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < CHUNK_ACK_TIMEOUT) {
                if (chunkAcks[key] == true) {
                    acknowledged = true
                    break
                }
                delay(100)
            }

            if (!acknowledged) {
                attempt++
                delay(INITIAL_BACKOFF * attempt)
            }
        }

        chunkAcks.remove(key)
    }

    private suspend fun sendWithRetry(message: Message) {
        val json = gson.toJson(message)
        var attempt = 0
        var success = false
        var backoff = INITIAL_BACKOFF
        
        val maxRetries = if (message.type == MessageType.SOS) SOS_MAX_RETRIES else MAX_RETRIES

        while (attempt < maxRetries && !success) {
            val connectedIds = transportLayer.getConnectedIds()
            if (connectedIds.isNotEmpty()) {
                transportLayer.sendToAll(json)
                success = true
            } else {
                attempt++
                if (attempt < maxRetries) {
                    delay(backoff)
                    backoff *= 2
                }
            }
        }
    }
}
