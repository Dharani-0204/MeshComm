package com.meshcomm.data.repository

import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.db.MessageEntity
import com.meshcomm.data.db.SeenMessageEntity
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageStatus
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepository(private val db: AppDatabase) {

    suspend fun saveMessage(message: Message) {
        db.messageDao().insert(message.toEntity())
    }

    fun getBroadcastMessages(): Flow<List<Message>> =
        db.messageDao().getBroadcastMessages().map { list -> list.map { it.toDomain() } }

    fun getDirectMessages(userId: String): Flow<List<Message>> =
        db.messageDao().getDirectMessages(userId).map { list -> list.map { it.toDomain() } }

    fun getSOSMessages(): Flow<List<Message>> =
        db.messageDao().getSOSMessages().map { list -> list.map { it.toDomain() } }

    fun getAllMessages(): Flow<List<Message>> =
        db.messageDao().getAllMessages().map { list -> list.map { it.toDomain() } }

    suspend fun markSeen(messageId: String) {
        db.seenMessageDao().markSeen(SeenMessageEntity(messageId))
    }

    suspend fun isSeen(messageId: String): Boolean =
        db.seenMessageDao().isSeen(messageId) > 0

    suspend fun updateStatus(messageId: String, status: MessageStatus) {
        db.messageDao().updateStatus(messageId, status.name)
    }

    private fun Message.toEntity() = MessageEntity(
        messageId = messageId,
        senderId = senderId,
        senderName = senderName,
        targetId = targetId,
        type = type.name,
        content = content,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        batteryLevel = batteryLevel,
        nearbyDevicesCount = nearbyDevicesCount,
        timestamp = timestamp,
        ttl = ttl,
        status = status.name,
        isEncrypted = isEncrypted
    )

    private fun MessageEntity.toDomain() = Message(
        messageId = messageId,
        senderId = senderId,
        senderName = senderName,
        targetId = targetId,
        type = MessageType.valueOf(type),
        content = content,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        batteryLevel = batteryLevel,
        nearbyDevicesCount = nearbyDevicesCount,
        timestamp = timestamp,
        ttl = ttl,
        status = MessageStatus.valueOf(status),
        isEncrypted = isEncrypted
    )
}
