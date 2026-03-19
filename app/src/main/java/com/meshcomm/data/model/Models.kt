package com.meshcomm.data.model

import java.util.UUID

enum class MessageType { NORMAL, SOS }
enum class MessageStatus { SENT, RELAYED, DELIVERED }
enum class UserRole { CIVILIAN, RESCUER, AUTHORITY }

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val targetId: String? = null,
    val type: MessageType = MessageType.NORMAL,
    val content: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val batteryLevel: Int = 100,
    val nearbyDevicesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 7,
    val status: MessageStatus = MessageStatus.SENT,
    val isEncrypted: Boolean = false
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
