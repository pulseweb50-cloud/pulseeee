package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PulseViewModel(private val repository: Repository) : ViewModel() {

    // Observe active list of users
    val users: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observe active list of channels
    val channels: StateFlow<List<Channel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observe current logged in user
    val currentUser: StateFlow<User?> = repository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current screen's cipher visibility setting
    private val _showCipherInChats = MutableStateFlow(false)
    val showCipherInChats: StateFlow<Boolean> = _showCipherInChats.asStateFlow()

    fun toggleCipherVisibility() {
        _showCipherInChats.value = !_showCipherInChats.value
    }

    /**
     * Changes the presence state of the current user.
     */
    fun updatePresence(status: String, customStatus: String) {
        viewModelScope.launch {
            val myUsername = currentUser.value?.username ?: "me"
            repository.updateStatus(myUsername, status, customStatus)
        }
    }

    /**
     * Changes the user profile name and color.
     */
    fun updateProfile(displayName: String, avatarColor: Int) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val updated = me.copy(displayName = displayName, avatarColor = avatarColor)
            repository.insertUser(updated)
        }
    }

    /**
     * Retrieves messages for a direct chat on-the-fly.
     */
    fun getMessagesWith(username: String): Flow<List<Message>> {
        return currentUser.flatMapLatest { me ->
            val myUsername = me?.username ?: "me"
            repository.getMessagesBetween(myUsername, username)
        }
    }

    /**
     * Retrieves messages for a specific channel.
     */
    fun getChannelMessages(channelId: String): Flow<List<Message>> {
        return repository.getChannelMessages(channelId)
    }

    /**
     * Tracks the user's navigation focus to pause/resume push notifications for active conversations.
     */
    fun setFocusedChat(username: String?) {
        repository.activeChatScreenUsername = username
        if (username != null) {
            viewModelScope.launch {
                val myUsername = currentUser.value?.username ?: "me"
                repository.markMessagesAsRead(myUsername, username)
            }
        }
    }

    /**
     * Encrypts and sends a message to a user or a broadcast channel.
     */
    fun sendMessage(receiver: String, text: String, isChannel: Boolean) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val myUsername = currentUser.value?.username ?: "me"
            repository.sendMessage(myUsername, receiver, text, isChannel)
        }
    }

    /**
     * Starts a secure chat session with a newly searched username.
     */
    fun startChatWithUser(username: String, displayName: String) {
        val sanitizedUsername = username.trim().lowercase().removePrefix("@")
        if (sanitizedUsername.isEmpty() || sanitizedUsername == "me") return
        viewModelScope.launch {
            val myUsername = currentUser.value?.username ?: "me"
            if (sanitizedUsername == myUsername) return@launch
            val existing = repository.allUsers.firstOrNull()?.find { it.username == sanitizedUsername }
            if (existing == null) {
                val newUser = User(
                    username = sanitizedUsername,
                    displayName = displayName.ifEmpty { "@$sanitizedUsername" },
                    status = "offline",
                    customStatus = "No status updated yet",
                    avatarColor = (0xFF000000.toLong() or (Math.random() * 0xFFFFFF).toLong()).toInt()
                )
                repository.insertUser(newUser)
            }
        }
    }

    /**
     * Creates a new public channel.
     */
    fun createChannel(id: String, name: String, description: String) {
        if (id.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            val myUsername = currentUser.value?.username ?: "me"
            repository.createChannel(id, name, description, myUsername)
        }
    }

    /**
     * Self-destruct logic (clears all messages from SQLite database securely).
     */
    fun clearLocalCache(context: Context) {
        viewModelScope.launch {
            repository.clearAllTables()
            Log.d("PulseViewModel", "Local cache cleared securely.")
        }
    }

    fun observeUser(username: String): Flow<User?> = repository.observeUser(username)
    fun observeChannel(channelId: String): Flow<Channel?> = repository.observeChannel(channelId)

    // Dynamic presence observables for contact cards
    fun observeLastMessage(otherUsername: String): Flow<Message?> {
        return currentUser.flatMapLatest { me ->
            val myUsername = me?.username ?: "me"
            repository.observeLastMessageBetween(myUsername, otherUsername)
        }
    }

    fun observeLastChannelMessage(channelId: String) = repository.observeLastChannelMessage(channelId)

    // --- Firebase Authentication Methods ---

    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Login failed")
            }
    }

    fun registerUser(
        email: String,
        password: String,
        username: String,
        displayName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sanitizedUsername = username.trim().lowercase()
        if (sanitizedUsername.isEmpty() || sanitizedUsername == "me") {
            onError("Invalid username")
            return
        }

        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(sanitizedUsername).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    onError("Username already taken")
                } else {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid ?: ""
                            val dto = FirebaseUserDto(
                                username = sanitizedUsername,
                                displayName = displayName.ifEmpty { "@$sanitizedUsername" },
                                email = email,
                                uid = uid,
                                status = "online",
                                customStatus = "Coding Pulse Messenger... 🚀",
                                avatarColor = (0xFF000000.toLong() or (Math.random() * 0xFFFFFF).toLong()).toInt()
                            )
                            usersRef.child(sanitizedUsername).setValue(dto)
                                .addOnSuccessListener {
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    onError(e.localizedMessage ?: "Failed to save profile")
                                }
                        }
                        .addOnFailureListener { e ->
                            onError(e.localizedMessage ?: "Registration failed")
                        }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

    fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
    }

    fun updateFirebaseConfig(context: Context, url: String, apiKey: String, projectId: String, appId: String) {
        val prefs = context.getSharedPreferences("pulse_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("firebase_db_url", url.trim())
            putString("firebase_api_key", apiKey.trim())
            putString("firebase_project_id", projectId.trim())
            putString("firebase_app_id", appId.trim())
            apply()
        }
        
        try {
            // Delete previous default FirebaseApp instance if any
            try {
                FirebaseApp.getInstance().delete()
            } catch (e: Exception) {
                // Ignore if not initialized
            }

            // Create new options
            val options = com.google.firebase.FirebaseOptions.Builder()
                .setApiKey(apiKey.trim().ifEmpty { "placeholder-api-key" })
                .setApplicationId(appId.trim().ifEmpty { "1:1234567890:android:abcdef123456" })
                .setProjectId(projectId.trim().ifEmpty { "pulse-messenger-placeholder" })
                .setDatabaseUrl(url.trim().ifEmpty { "https://pulse-messenger-placeholder-default-rtdb.firebaseio.com" })
                .build()

            FirebaseApp.initializeApp(context.applicationContext, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        repository.startFirebaseSync()
    }

    fun getFirebaseDbUrl(context: Context): String {
        val prefs = context.getSharedPreferences("pulse_prefs", Context.MODE_PRIVATE)
        return prefs.getString("firebase_db_url", "") ?: ""
    }

    fun getFirebaseApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("pulse_prefs", Context.MODE_PRIVATE)
        return prefs.getString("firebase_api_key", "") ?: ""
    }

    fun getFirebaseProjectId(context: Context): String {
        val prefs = context.getSharedPreferences("pulse_prefs", Context.MODE_PRIVATE)
        return prefs.getString("firebase_project_id", "") ?: ""
    }

    fun getFirebaseAppId(context: Context): String {
        val prefs = context.getSharedPreferences("pulse_prefs", Context.MODE_PRIVATE)
        return prefs.getString("firebase_app_id", "") ?: ""
    }
}

class PulseViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PulseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PulseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
