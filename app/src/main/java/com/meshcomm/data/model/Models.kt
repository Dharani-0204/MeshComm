package com.meshcomm.data.model

import java.util.UUID

enum class MessageType { BROADCAST, DIRECT, SOS, INFO, HANDSHAKE }
enum class MessageStatus { SENT, RELAYED, DELIVERED }
enum class UserRole { CIVILIAN, RESCUER, AUTHORITY }

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val targetId: String? = null, // null for broadcast, specific ID for direct messages
    val type: MessageType = MessageType.BROADCAST,
    val content: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val batteryLevel: Int = 100,
    val nearbyDevicesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 7,
    val status: MessageStatus = MessageStatus.SENT,
    val isEncrypted: Boolean = false,
    val deviceId: String = "" // Device identifier for self-filtering
)

// Mesh-first payload (used for Bluetooth relays)
data class MeshMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val userId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val messageType: MessageType,
    val ttl: Int,
    val content: String,
    val batteryLevel: Int = 100,
    val deviceId: String = ""
)

data class User(
    val userId: String,
    val name: String,
    val role: UserRole = UserRole.CIVILIAN,
    val emergencyContacts: List<String> = emptyList()
)

data class PeerDevice(
    val deviceId: String,
    var deviceName: String,
    val transport: TransportType,
    val rssi: Int = 0,
    var batteryLevel: Int = 100,
    var role: UserRole = UserRole.CIVILIAN,
    var isConnected: Boolean = false,
    val connectedAt: Long = System.currentTimeMillis(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var lastSeen: Long = System.currentTimeMillis(),
    var isSosActive: Boolean = false
)

enum class TransportType { BLUETOOTH, WIFI_DIRECT }

data class MeshStats(
    val connectedCount: Int,
    val relayedCount: Int,
    val isActive: Boolean
)

// ─── Emergency Profile Models ────────────────────────────────────────────────

data class UserProfile(
    val userId: String,
    val name: String,
    val role: UserRole,
    val bloodGroup: String = "",
    val medicalConditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val isProfileComplete: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class EmergencyContact(
    val name: String,
    val phone: String,
    val relationship: String = ""
)

// ─── SOS Alert Models ────────────────────────────────────────────────────────

data class SOSAlert(
    val alertId: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val batteryLevel: Int,
    val isActive: Boolean = true,
    val profile: UserProfile? = null  // Attached profile for civilians
)

// ─── Dashboard Models ────────────────────────────────────────────────────────

data class SOSCluster(
    val centerLat: Double,
    val centerLng: Double,
    val alerts: List<SOSAlert>,
    val count: Int
)

data class ActiveDevice(
    val deviceId: String,
    val userName: String,
    val role: UserRole,
    val latitude: Double,
    val longitude: Double,
    val lastSeen: Long,
    val batteryLevel: Int
)

data class MovementPath(
    val userId: String,
    val userName: String,
    val points: List<LocationPoint>
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
