package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String, // e.g. "pidor", "alex", "julia"
    val displayName: String,
    val status: String = "online", // "online", "idle", "dnd", "offline"
    val customStatus: String = "",
    val avatarColor: Int = 0xFF2196F3.toInt(), // hex color code
    val isCurrentUser: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String, // push key or UUID
    val senderId: String,
    val receiverId: String, // Username or ChannelId
    val encryptedText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isChannelMessage: Boolean = false,
    val isRead: Boolean = false
)

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String, // e.g., "pulse_news"
    val name: String,
    val description: String,
    val ownerUsername: String,
    val memberCount: Int = 1
)
