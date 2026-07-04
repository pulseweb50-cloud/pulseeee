package com.example.data

import android.content.Context
import android.util.Log
import com.example.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.crypto.spec.SecretKeySpec

// Firebase Data Transfer Objects
data class FirebaseUserDto(
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val uid: String = "",
    val status: String = "offline",
    val customStatus: String = "",
    val avatarColor: Int = 0xFF2196F3.toInt()
)

data class FirebaseMessageDto(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val encryptedText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isChannelMessage: Boolean = false,
    val isRead: Boolean = false
)

data class FirebaseChannelDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerUsername: String = "",
    val memberCount: Int = 1
)

class Repository(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private val userDao = database.userDao()
    private val messageDao = database.messageDao()
    private val channelDao = database.channelDao()

    @Volatile
    var activeUsername: String = "me"

    // Keep track of the currently active chat screen to avoid sending push notifications while active in the chat
    @Volatile
    var activeChatScreenUsername: String? = null

    // Firebase listeners
    private var usersListener: ChildEventListener? = null
    private var channelsListener: ChildEventListener? = null
    private var messagesListener: ChildEventListener? = null

    // Users and Channels observables
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels()

    fun observeUser(username: String): Flow<User?> = userDao.observeUserByUsername(username)
    fun observeChannel(id: String): Flow<Channel?> = channelDao.observeChannelById(id)
    fun observeCurrentUser(): Flow<User?> = userDao.observeCurrentUser()

    init {
        // Automatically start/stop real-time sync when FirebaseAuth changes
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                scope.launch(Dispatchers.IO) {
                    // Check if there is already a user node matching this UID
                    val usersRef = getDatabaseRef("users")
                    usersRef.orderByChild("uid").equalTo(firebaseUser.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val child = snapshot.children.firstOrNull()
                                    val dto = child?.getValue(FirebaseUserDto::class.java)
                                    if (dto != null) {
                                        activeUsername = dto.username
                                        scope.launch(Dispatchers.IO) {
                                            userDao.clearCurrentUserFlags()
                                            val localUser = User(
                                                username = dto.username,
                                                displayName = dto.displayName,
                                                status = dto.status,
                                                customStatus = dto.customStatus,
                                                avatarColor = dto.avatarColor,
                                                isCurrentUser = true
                                            )
                                            userDao.insertUser(localUser)
                                        }
                                    }
                                }
                                startFirebaseSync()
                            }
                            override fun onCancelled(error: DatabaseError) {
                                startFirebaseSync()
                            }
                        })
                }
            } else {
                stopFirebaseSync()
                activeUsername = "me"
                scope.launch(Dispatchers.IO) {
                    userDao.clearCurrentUserFlags()
                }
            }
        }
    }

    private fun getDatabaseRef(path: String): DatabaseReference {
        val prefs = context.getSharedPreferences("pulse_prefs", Context.MODE_PRIVATE)
        val customUrl = prefs.getString("firebase_db_url", null)
        return if (!customUrl.isNullOrBlank()) {
            try {
                FirebaseDatabase.getInstance(customUrl).getReference(path)
            } catch (e: Exception) {
                FirebaseDatabase.getInstance().getReference(path)
            }
        } else {
            try {
                FirebaseDatabase.getInstance().getReference(path)
            } catch (e: Exception) {
                FirebaseDatabase.getInstance().getReference(path)
            }
        }
    }

    fun startFirebaseSync() {
        stopFirebaseSync()

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        // 1. Sync Users from Firebase to local Room DB
        val usersRef = getDatabaseRef("users")
        usersListener = usersRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                scope.launch(Dispatchers.IO) {
                    val dto = snapshot.getValue(FirebaseUserDto::class.java) ?: return@launch
                    val isMe = (dto.uid == currentUid)
                    if (isMe) {
                        activeUsername = dto.username
                    }
                    val user = User(
                        username = dto.username,
                        displayName = dto.displayName,
                        status = dto.status,
                        customStatus = dto.customStatus,
                        avatarColor = dto.avatarColor,
                        isCurrentUser = isMe
                    )
                    userDao.insertUser(user)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, prev: String?) {
                scope.launch(Dispatchers.IO) {
                    val dto = snapshot.getValue(FirebaseUserDto::class.java) ?: return@launch
                    val isMe = (dto.uid == currentUid)
                    val user = User(
                        username = dto.username,
                        displayName = dto.displayName,
                        status = dto.status,
                        customStatus = dto.customStatus,
                        avatarColor = dto.avatarColor,
                        isCurrentUser = isMe
                    )
                    userDao.insertUser(user)
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, prev: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Sync Channels from Firebase to local Room DB
        val channelsRef = getDatabaseRef("channels")
        channelsListener = channelsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                scope.launch(Dispatchers.IO) {
                    val dto = snapshot.getValue(FirebaseChannelDto::class.java) ?: return@launch
                    val channel = Channel(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        ownerUsername = dto.ownerUsername,
                        memberCount = dto.memberCount
                    )
                    channelDao.insertChannel(channel)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, prev: String?) {
                scope.launch(Dispatchers.IO) {
                    val dto = snapshot.getValue(FirebaseChannelDto::class.java) ?: return@launch
                    val channel = Channel(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        ownerUsername = dto.ownerUsername,
                        memberCount = dto.memberCount
                    )
                    channelDao.insertChannel(channel)
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, prev: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        // 3. Sync Messages from Firebase to local Room DB
        val messagesRef = getDatabaseRef("messages")
        messagesListener = messagesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                scope.launch(Dispatchers.IO) {
                    val dto = snapshot.getValue(FirebaseMessageDto::class.java) ?: return@launch
                    val msg = Message(
                        id = dto.id,
                        senderId = dto.senderId,
                        receiverId = dto.receiverId,
                        encryptedText = dto.encryptedText,
                        timestamp = dto.timestamp,
                        isChannelMessage = dto.isChannelMessage,
                        isRead = dto.isRead
                    )
                    messageDao.insertMessage(msg)

                    // Show push notifications for direct messages from others
                    val myUsername = activeUsername
                    if (dto.senderId != myUsername && !dto.isChannelMessage && dto.receiverId == myUsername) {
                        val senderUser = userDao.getUserByUsername(dto.senderId)
                        val senderName = senderUser?.displayName ?: dto.senderId
                        val key = EncryptionHelper.getSecretKey(myUsername, dto.senderId)
                        val decrypted = try {
                            EncryptionHelper.decrypt(dto.encryptedText, key)
                        } catch (e: Exception) {
                            "[Encrypted Message]"
                        }
                        if (activeChatScreenUsername != dto.senderId) {
                            NotificationHelper.showNotification(context, senderName, decrypted)
                        }
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, prev: String?) {
                scope.launch(Dispatchers.IO) {
                    val dto = snapshot.getValue(FirebaseMessageDto::class.java) ?: return@launch
                    val msg = Message(
                        id = dto.id,
                        senderId = dto.senderId,
                        receiverId = dto.receiverId,
                        encryptedText = dto.encryptedText,
                        timestamp = dto.timestamp,
                        isChannelMessage = dto.isChannelMessage,
                        isRead = dto.isRead
                    )
                    messageDao.insertMessage(msg)
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, prev: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun stopFirebaseSync() {
        usersListener?.let { getDatabaseRef("users").removeEventListener(it) }
        channelsListener?.let { getDatabaseRef("channels").removeEventListener(it) }
        messagesListener?.let { getDatabaseRef("messages").removeEventListener(it) }
        usersListener = null
        channelsListener = null
        messagesListener = null
    }

    suspend fun clearAllTables() {
        database.clearAllTables()
    }

    fun getMessagesBetween(currentUser: String, otherUser: String): Flow<List<Message>> {
        return messageDao.getMessagesBetween(currentUser, otherUser)
    }

    fun getChannelMessages(channelId: String): Flow<List<Message>> {
        return messageDao.getChannelMessages(channelId)
    }

    fun observeLastMessageBetween(user1: String, user2: String): Flow<Message?> {
        return messageDao.observeLastMessageBetween(user1, user2)
    }

    fun observeLastChannelMessage(channelId: String): Flow<Message?> {
        return messageDao.observeLastChannelMessage(channelId)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
        if (user.isCurrentUser) {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                val userRef = getDatabaseRef("users").child(user.username)
                userRef.child("displayName").setValue(user.displayName)
                userRef.child("avatarColor").setValue(user.avatarColor)
            }
        }
    }

    suspend fun updateStatus(username: String, status: String, customStatus: String) {
        userDao.updateStatus(username, status, customStatus)
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            val userRef = getDatabaseRef("users").child(username)
            userRef.child("status").setValue(status)
            userRef.child("customStatus").setValue(customStatus)
        }
    }

    suspend fun createChannel(id: String, name: String, description: String, owner: String) {
        val channelId = id.trim().lowercase().replace("\\s+".toRegex(), "_")
        val dto = FirebaseChannelDto(
            id = channelId,
            name = name,
            description = description,
            ownerUsername = owner,
            memberCount = 1
        )
        getDatabaseRef("channels").child(channelId).setValue(dto)
    }

    suspend fun sendMessage(sender: String, receiver: String, plainText: String, isChannel: Boolean) {
        val messagesRef = getDatabaseRef("messages")
        val messageId = messagesRef.push().key ?: java.util.UUID.randomUUID().toString()

        val key: SecretKeySpec = if (isChannel) {
            EncryptionHelper.getChannelKey(receiver)
        } else {
            EncryptionHelper.getSecretKey(sender, receiver)
        }

        val encryptedText = EncryptionHelper.encrypt(plainText, key)
        val dto = FirebaseMessageDto(
            id = messageId,
            senderId = sender,
            receiverId = receiver,
            encryptedText = encryptedText,
            timestamp = System.currentTimeMillis(),
            isChannelMessage = isChannel,
            isRead = false
        )

        messagesRef.child(messageId).setValue(dto)

        // Trigger bot reply if message is sent to pulse_bot
        if (!isChannel && receiver == "pulse_bot") {
            scope.launch(Dispatchers.IO) {
                simulateReply(sender, receiver)
            }
        }
    }

    private suspend fun simulateReply(sender: String, receiver: String) {
        val receiverUser = userDao.getUserByUsername(receiver) ?: return
        val originalCustomStatus = receiverUser.customStatus
        
        updateStatus(receiver, "online", "typing...")
        delay(2000)

        val messages = messageDao.getMessagesBetween(sender, receiver).firstOrNull() ?: emptyList()
        val key = EncryptionHelper.getSecretKey(sender, receiver)

        val plainHistory = messages.map { msg ->
            val plain = EncryptionHelper.decrypt(msg.encryptedText, key)
            val senderLabel = if (msg.senderId == sender) "me" else receiver
            Pair(senderLabel, plain)
        }

        val replyPlain = GeminiService.generateReply(receiver, receiverUser.displayName, plainHistory)
        val replyEncrypted = EncryptionHelper.encrypt(replyPlain, key)

        val messagesRef = getDatabaseRef("messages")
        val replyId = messagesRef.push().key ?: java.util.UUID.randomUUID().toString()

        val replyDto = FirebaseMessageDto(
            id = replyId,
            senderId = receiver,
            receiverId = sender,
            encryptedText = replyEncrypted,
            timestamp = System.currentTimeMillis(),
            isChannelMessage = false,
            isRead = false
        )
        messagesRef.child(replyId).setValue(replyDto)

        updateStatus(receiver, receiverUser.status, originalCustomStatus)
    }

    suspend fun markMessagesAsRead(receiverId: String, senderId: String) {
        messageDao.markMessagesAsRead(receiverId, senderId)
    }
}
