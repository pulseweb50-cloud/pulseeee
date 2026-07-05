package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY isCurrentUser DESC, username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun observeUserByUsername(username: String): Flow<User?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<User?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUserFlags()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET status = :status, customStatus = :customStatus WHERE username = :username")
    suspend fun updateStatus(username: String, status: String, customStatus: String)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface MessageDao {
    @Query("""
        SELECT * FROM messages 
        WHERE isChannelMessage = 0 
        AND ((senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1))
        ORDER BY timestamp ASC
    """)
    fun getMessagesBetween(user1: String, user2: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE isChannelMessage = 1 AND receiverId = :channelId ORDER BY timestamp ASC")
    fun getChannelMessages(channelId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("""
        SELECT * FROM messages 
        WHERE isChannelMessage = 0 
        AND ((senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1))
        ORDER BY timestamp DESC LIMIT 1
    """)
    fun observeLastMessageBetween(user1: String, user2: String): Flow<Message?>

    @Query("SELECT * FROM messages WHERE isChannelMessage = 1 AND receiverId = :channelId ORDER BY timestamp DESC LIMIT 1")
    fun observeLastChannelMessage(channelId: String): Flow<Message?>

    @Query("UPDATE messages SET isRead = 1 WHERE receiverId = :receiverId AND senderId = :senderId")
    suspend fun markMessagesAsRead(receiverId: String, senderId: String)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): Channel?

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    fun observeChannelById(id: String): Flow<Channel?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)

    @Query("UPDATE channels SET memberCount = memberCount + 1 WHERE id = :channelId")
    suspend fun incrementMemberCount(channelId: String)
}
