package com.meshcomm.mesh

import android.content.Context
import android.util.Base64
import android.util.Log
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageStatus
import com.meshcomm.data.model.MessageType
import com.meshcomm.utils.PrefsHelper
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ChunkingManager(private val context: Context) {
    companion object {
        private const val TAG = "ChunkingManager"
        private const val CHUNK_SIZE = 8 * 1024 // 8KB for better reliability
    }

    // In-memory storage for reassembling chunks: originalMessageId -> Map(chunkIndex -> payload)
    private val incomingChunks = ConcurrentHashMap<String, MutableMap<Int, String>>()
    private val expectedChunks = ConcurrentHashMap<String, Int>()
    private val originalMessages = ConcurrentHashMap<String, Message>()

    /**
     * Splits a large message into chunks.
     * Returns a list of Message objects of type CHUNK.
     */
    fun chunkify(message: Message): List<Message> {
        val mediaPath = message.mediaUri ?: return listOf(message)
        val file = File(mediaPath)
        if (!file.exists()) {
            Log.e(TAG, "File not found for chunking: $mediaPath")
            return listOf(message)
        }

        Log.d(TAG, "Starting chunking for file: ${file.name}, size: ${file.length()} bytes")
        
        val bytes = file.readBytes()
        // FIX: Use NO_WRAP to avoid newlines breaking the transport layer's readLine()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val totalChunks = (base64.length + CHUNK_SIZE - 1) / CHUNK_SIZE

        val chunks = mutableListOf<Message>()
        for (i in 0 until totalChunks) {
            val start = i * CHUNK_SIZE
            val end = minOf(start + CHUNK_SIZE, base64.length)
            val payload = base64.substring(start, end)

            chunks.add(
                Message(
                    messageId = UUID.randomUUID().toString(),
                    senderId = message.senderId,
                    senderName = message.senderName,
                    targetId = message.targetId,
                    type = MessageType.CHUNK,
                    content = payload,
                    timestamp = message.timestamp,
                    deviceId = message.deviceId,
                    chunkIndex = i,
                    totalChunks = totalChunks,
                    originalMessageId = message.messageId,
                    mediaDuration = message.mediaDuration,
                    mediaType = message.mediaType,
                    fileName = message.fileName,
                    latitude = message.latitude,
                    longitude = message.longitude,
                    batteryLevel = message.batteryLevel,
                    nearbyDevicesCount = message.nearbyDevicesCount
                )
            )
        }
        
        Log.i(TAG, "Chunkified ${message.messageId} into $totalChunks chunks")
        return chunks
    }

    /**
     * Processes an incoming chunk. 
     * Returns the fully reassembled Message if all chunks are present, null otherwise.
     */
    fun processChunk(chunk: Message, onAckNeeded: (Message) -> Unit): Message? {
        val originalId = chunk.originalMessageId ?: return null
        
        // Send ACK for this chunk
        sendChunkAck(chunk, onAckNeeded)
        
        val chunksMap = incomingChunks.getOrPut(originalId) { ConcurrentHashMap<Int, String>() }
        
        // Deduplication for chunks
        if (chunksMap.containsKey(chunk.chunkIndex)) {
            Log.d(TAG, "Duplicate chunk ${chunk.chunkIndex} for $originalId ignored")
            return null
        }
        
        chunksMap[chunk.chunkIndex] = chunk.content
        expectedChunks[originalId] = chunk.totalChunks
        
        if (!originalMessages.containsKey(originalId)) {
            originalMessages[originalId] = chunk
        }

        if (chunksMap.size == chunk.totalChunks) {
            Log.i(TAG, "All ${chunk.totalChunks} chunks received for $originalId. Reassembling...")
            return reassemble(originalId)
        }
        
        Log.d(TAG, "Progress for $originalId: ${chunksMap.size}/${chunk.totalChunks}")
        return null
    }

    private fun sendChunkAck(chunk: Message, onAckNeeded: (Message) -> Unit) {
        val ack = Message(
            senderId = PrefsHelper.getUserId(context),
            senderName = PrefsHelper.getUserName(context),
            targetId = chunk.senderId,
            type = MessageType.CHUNK_ACK,
            content = "ACK",
            originalMessageId = chunk.originalMessageId,
            chunkIndex = chunk.chunkIndex,
            deviceId = PrefsHelper.getUserId(context)
        )
        onAckNeeded(ack)
    }

    private fun reassemble(originalId: String): Message? {
        val chunksMap = incomingChunks.remove(originalId) ?: return null
        val total = expectedChunks.remove(originalId) ?: return null
        val metadata = originalMessages.remove(originalId) ?: return null

        val sb = StringBuilder()
        for (i in 0 until total) {
            val part = chunksMap[i] ?: run {
                Log.e(TAG, "Missing chunk $i during reassembly for $originalId")
                return null
            }
            sb.append(part)
        }

        return try {
            val fullBase64 = sb.toString()
            val bytes = Base64.decode(fullBase64, Base64.NO_WRAP)
            
            val isAudio = metadata.type == MessageType.AUDIO || metadata.mediaType?.contains("audio") == true || metadata.mediaDuration > 0
            val typeSuffix = if (isAudio) "audio" else "images"
            val extension = if (isAudio) ".m4a" else ".jpg"
            
            val folder = File(context.filesDir, "MeshComm/$typeSuffix")
            if (!folder.exists()) folder.mkdirs()
            
            val file = File(folder, "recv_${System.currentTimeMillis()}$extension")
            file.writeBytes(bytes)

            Log.i(TAG, "Reassembly successful: ${file.absolutePath} (${bytes.size} bytes)")

            Message(
                messageId = originalId,
                senderId = metadata.senderId,
                senderName = metadata.senderName,
                targetId = metadata.targetId,
                type = if (isAudio) MessageType.AUDIO else MessageType.IMAGE,
                content = if (isAudio) "Audio message" else "Image message",
                mediaUri = file.absolutePath,
                mediaDuration = metadata.mediaDuration,
                mediaType = metadata.mediaType,
                fileName = file.name,
                timestamp = metadata.timestamp,
                deviceId = metadata.deviceId,
                latitude = metadata.latitude,
                longitude = metadata.longitude,
                batteryLevel = metadata.batteryLevel,
                nearbyDevicesCount = metadata.nearbyDevicesCount,
                status = MessageStatus.DELIVERED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reassemble $originalId: ${e.message}")
            null
        }
    }
}
