package com.meshcomm.data.db

import androidx.room.*
import com.meshcomm.data.model.MessageStatus
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.model.UserRole
import kotlinx.coroutines.flow.Flow

// ─── Entities ────────────────────────────────────────────────────────────────

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val senderName: String,
    val targetId: String?,
    val type: String,
    val content: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String = "",
    val batteryLevel: Int,
    val nearbyDevicesCount: Int,
    val timestamp: Long,
    val ttl: Int,
    val status: String,
    val isEncrypted: Boolean,
    val deviceId: String = ""
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val role: String,
    val emergencyContacts: String  // JSON array stored as string
)

@Entity(tableName = "seen_messages")
data class SeenMessageEntity(
    @PrimaryKey val messageId: String,
    val seenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val odiumId: String,
    val name: String,
    val role: String,
    val bloodGroup: String = "",
    val medicalConditions: String = "",  // JSON array stored as string
    val allergies: String = "",          // JSON array stored as string
    val emergencyContacts: String = "",  // JSON array stored as string
    val isProfileComplete: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncedWithFirebase: Boolean = false
)

@Entity(tableName = "sos_alerts")
data class SOSAlertEntity(
    @PrimaryKey val alertId: String,
    val userId: String,
    val userName: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: Long,
    val batteryLevel: Int,
    val isActive: Boolean = true,
    val profileSnapshot: String = "",  // JSON of profile at alert time
    val syncedWithFirebase: Boolean = false
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE targetId IS NULL ORDER BY timestamp DESC")
    fun getBroadcastMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE (senderId = :userId OR targetId = :userId) AND targetId IS NOT NULL ORDER BY timestamp ASC")
    fun getDirectMessages(userId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE type = 'SOS' ORDER BY timestamp DESC")
    fun getSOSMessages(): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("DELETE FROM messages WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}

@Dao
interface SeenMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeen(entity: SeenMessageEntity)

    @Query("SELECT COUNT(*) FROM seen_messages WHERE messageId = :messageId")
    suspend fun isSeen(messageId: String): Int

    @Query("DELETE FROM seen_messages WHERE seenAt < :cutoff")
    suspend fun cleanUp(cutoff: Long)
}

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE odiumId = :userId")
    suspend fun getProfile(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE odiumId = :userId")
    fun getProfileFlow(userId: String): Flow<UserProfileEntity?>

    @Query("UPDATE user_profiles SET isProfileComplete = :complete, lastUpdated = :timestamp WHERE odiumId = :userId")
    suspend fun updateProfileCompletion(userId: String, complete: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profiles SET syncedWithFirebase = :synced WHERE odiumId = :userId")
    suspend fun updateSyncStatus(userId: String, synced: Boolean)

    @Query("SELECT * FROM user_profiles WHERE syncedWithFirebase = 0")
    suspend fun getUnsyncedProfiles(): List<UserProfileEntity>
}

@Dao
interface SOSAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: SOSAlertEntity)

    @Query("SELECT * FROM sos_alerts WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<SOSAlertEntity>>

    @Query("SELECT * FROM sos_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<SOSAlertEntity>>

    @Query("SELECT * FROM sos_alerts ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAlerts(limit: Int): Flow<List<SOSAlertEntity>>

    @Query("UPDATE sos_alerts SET isActive = :active WHERE alertId = :alertId")
    suspend fun updateActiveStatus(alertId: String, active: Boolean)

    @Query("UPDATE sos_alerts SET syncedWithFirebase = :synced WHERE alertId = :alertId")
    suspend fun updateSyncStatus(alertId: String, synced: Boolean)

    @Query("SELECT * FROM sos_alerts WHERE syncedWithFirebase = 0")
    suspend fun getUnsyncedAlerts(): List<SOSAlertEntity>

    @Query("SELECT * FROM sos_alerts WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getAlertsSince(since: Long): Flow<List<SOSAlertEntity>>
}
