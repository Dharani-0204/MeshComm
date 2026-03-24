package com.meshcomm.data.model

import java.util.UUID

enum class MessageType { BROADCAST, DIRECT, SOS, AUDIO, IMAGE, CHUNK, CHUNK_ACK }
enum class MessageStatus { SENT, RELAYED, DELIVERED, PENDING, FAILED }
enum class UserRole { CIVILIAN, RESCUER, AUTHORITY }

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val targetId: String? = null, // null for broadcast
    val type: MessageType = MessageType.BROADCAST,
    val content: String, // Text message or Chunk Base64 payload
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val batteryLevel: Int = 100,
    val nearbyDevicesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 7,
    var status: MessageStatus = MessageStatus.SENT,
    val isEncrypted: Boolean = false,
    val deviceId: String = "", 
    
    // Media extensions
    val mediaUri: String? = null, // Local path
    val mediaDuration: Long = 0, // For audio
    val fileName: String? = null,
    val mediaType: String? = null, // "image/jpeg", "audio/m4a"
    
    // Chunking extensions
    val chunkIndex: Int = 0,
    val totalChunks: Int = 1,
    val originalMessageId: String? = null // To reassemble chunks
)

data class User(
    val userId: String,
    val name: String,
    val role: UserRole = UserRole.CIVILIAN,
    val emergencyContacts: List<String> = emptyList()
)

data class PeerDevice(
    val deviceId: String,
    val deviceName: String,
    val transport: TransportType,
    val rssi: Int = 0,
    val batteryLevel: Int = 100,
    val role: UserRole = UserRole.CIVILIAN,
    var isConnected: Boolean = false,
    val connectedAt: Long = System.currentTimeMillis()
)

enum class TransportType { BLUETOOTH, WIFI_DIRECT }

data class MeshStats(
    val connectedCount: Int,
    val relayedCount: Int,
    val isActive: Boolean
)
