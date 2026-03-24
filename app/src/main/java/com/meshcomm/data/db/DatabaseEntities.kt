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
    val batteryLevel: Int,
    val nearbyDevicesCount: Int,
    val timestamp: Long,
    val ttl: Int,
    val status: String,
    val isEncrypted: Boolean,
    val deviceId: String = "",
    
    // Media extensions
    val mediaUri: String? = null,
    val mediaDuration: Long = 0,
    val fileName: String? = null,
    val mediaType: String? = null,
    
    // Chunking extensions
    val chunkIndex: Int = 0,
    val totalChunks: Int = 1,
    val originalMessageId: String? = null
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
