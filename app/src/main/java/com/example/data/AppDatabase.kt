package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Message::class, Channel::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun channelDao(): ChannelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pulse_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            val userDao = db.userDao()
            val channelDao = db.channelDao()
            val messageDao = db.messageDao()

            // Seed Users
            val currentUser = User(
                username = "me",
                displayName = "I Ilya",
                status = "online",
                customStatus = "Coding Pulse Messenger... 🚀",
                avatarColor = 0xFF00E5FF.toInt(), // Vibrant Cyan
                isCurrentUser = true
            )
            val pidor = User(
                username = "pidor",
                displayName = "Mykola @pidor",
                status = "idle",
                customStatus = "Chill and chat 🎧",
                avatarColor = 0xFFFF3D00.toInt(), // Bold Red-Orange
                isCurrentUser = false
            )
            val alex = User(
                username = "alex",
                displayName = "Alex Reed",
                status = "dnd",
                customStatus = "Do not disturb - Coding",
                avatarColor = 0xFF7C4DFF.toInt(), // Purple Accent
                isCurrentUser = false
            )
            val julia = User(
                username = "julia",
                displayName = "Julia Volkova",
                status = "offline",
                customStatus = "Last seen recently",
                avatarColor = 0xFFFF4081.toInt(), // Pink Accent
                isCurrentUser = false
            )
            val bot = User(
                username = "pulse_bot",
                displayName = "Pulse AI Bot 🤖",
                status = "online",
                customStatus = "Ask me anything (Powered by Gemini!)",
                avatarColor = 0xFF00E676.toInt(), // Green Accent
                isCurrentUser = false
            )

            userDao.insertUser(currentUser)
            userDao.insertUser(pidor)
            userDao.insertUser(alex)
            userDao.insertUser(julia)
            userDao.insertUser(bot)

            // Seed Channels
            val pulseNews = Channel(
                id = "pulse_news",
                name = "Pulse Announcements",
                description = "Official updates and announcements from the Pulse Messenger team.",
                ownerUsername = "pulse_bot",
                memberCount = 1204
            )
            val devTalk = Channel(
                id = "dev_talk",
                name = "Android Development",
                description = "A public channel for Android developers using Jetpack Compose and Kotlin.",
                ownerUsername = "alex",
                memberCount = 512
            )

            channelDao.insertChannel(pulseNews)
            channelDao.insertChannel(devTalk)

            // Seed some encrypted welcome messages
            // Channel news greeting
            val channelKey = EncryptionHelper.getChannelKey("pulse_news")
            val encryptedNews = EncryptionHelper.encrypt(
                "Welcome to Pulse Messenger! 🚀\n\nThis app is fully optimized for high-performance and secured using state-of-the-art simulated End-to-End Encryption (E2EE). Stay tuned for feature updates!",
                channelKey
            )
            messageDao.insertMessage(
                Message(
                    id = "seed_news_greeting",
                    senderId = "pulse_bot",
                    receiverId = "pulse_news",
                    encryptedText = encryptedNews,
                    isChannelMessage = true
                )
            )

            // Welcome chat message from AI bot
            val botKey = EncryptionHelper.getSecretKey("me", "pulse_bot")
            val encryptedBotWelcome = EncryptionHelper.encrypt(
                "Hi! I am the Pulse AI Bot. Feel free to text me. I can respond to your messages instantly, powered by the secure Gemini AI! 💬 Let's check our secure E2EE connection together.",
                botKey
            )
            messageDao.insertMessage(
                Message(
                    id = "seed_bot_welcome",
                    senderId = "pulse_bot",
                    receiverId = "me",
                    encryptedText = encryptedBotWelcome,
                    isChannelMessage = false
                )
            )

            // Welcome chat message from Mykola @pidor
            val pidorKey = EncryptionHelper.getSecretKey("me", "pidor")
            val encryptedPidorWelcome = EncryptionHelper.encrypt(
                "Привіт! Здоров! Як справи? Я Mykola, мій юзернейм @pidor як ти просив у ТЗ 😂. Надішли мені повідомлення, щоб перевірити наскрізне шифрування!",
                pidorKey
            )
            messageDao.insertMessage(
                Message(
                    id = "seed_pidor_welcome",
                    senderId = "pidor",
                    receiverId = "me",
                    encryptedText = encryptedPidorWelcome,
                    isChannelMessage = false
                )
            )
        }
    }
}
