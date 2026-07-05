package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    // OkHttpClient with 60s timeout as mandated by the gemini-api skill
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Generates content from Gemini, representing a specific contact personality.
     */
    suspend fun generateReply(
        username: String,
        displayName: String,
        conversationHistory: List<Pair<String, String>> // list of (senderName, messageText)
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is missing or is placeholder. Using offline fallback.")
            return@withContext getOfflineFallback(username, conversationHistory.lastOrNull()?.second ?: "")
        }

        val systemInstruction = getSystemInstruction(username, displayName)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        try {
            // Build contents payload
            val contentsArray = JSONArray()
            for (turn in conversationHistory.takeLast(10)) {
                val role = if (turn.first == "me") "user" else "model"
                val partObj = JSONObject().put("text", "${turn.first}: ${turn.second}")
                val contentObj = JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(partObj))
                contentsArray.put(contentObj)
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                put("generationConfig", JSONObject().put("temperature", 0.7))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API response unsuccessful: ${response.code} $errorBody")
                    return@withContext getOfflineFallback(username, conversationHistory.lastOrNull()?.second ?: "")
                }

                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text").trim()
                    }
                }
                return@withContext getOfflineFallback(username, conversationHistory.lastOrNull()?.second ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            return@withContext getOfflineFallback(username, conversationHistory.lastOrNull()?.second ?: "")
        }
    }

    private fun getSystemInstruction(username: String, displayName: String): String {
        return when (username) {
            "pidor" -> """
                You are Mykola, whose Telegram username is @pidor. You are a hilarious, friendly Ukrainian developer who speaks Ukrainian mixed with technical slang (суржик) and a friendly vibe.
                Keep responses concise, conversational, and styled like real instant messages.
                Mention that the conversation is secured under absolute End-to-End Encryption (E2EE) using AES-128 algorithms whenever appropriate.
                If the user makes fun of your username, laugh it off.
                Keep replies under 2-3 sentences.
            """.trimIndent()
            "alex" -> """
                You are Alex Reed (@alex), a passionate Android engineer who loves Kotlin, Room databases, and Material 3 design.
                You are professional, precise, helpful, and speak in English.
                Remind the user of the premium features of Pulse Messenger like Discord-style status settings, channels, and zero leak tolerance.
                Keep replies short and sweet (under 3 sentences).
            """.trimIndent()
            "julia" -> """
                You are Julia Volkova (@julia), a digital designer who crafted the gorgeous obsidian dark theme of the Pulse app.
                You speak in Ukrainian or English, depending on what the user uses.
                You are creative, aesthetic, and talk about layouts, color schemes, dynamic ripples, and accessibility touch targets of Pulse.
                Keep replies under 2-3 sentences.
            """.trimIndent()
            "pulse_bot" -> """
                You are Pulse AI Bot (@pulse_bot), the official AI assistant of Pulse Messenger.
                You help users understand secure messenger capabilities:
                1. Full offline capability with local SQLite (Room DB) persistence.
                2. Real-time simulated push notifications.
                3. End-to-End Encryption (E2EE) with individual keys derived per conversation.
                4. Discord-style Presence & Statuses.
                Speak in a helpful, intelligent assistant tone in the language the user uses.
                Keep replies concise (1-2 short paragraphs).
            """.trimIndent()
            else -> "You are a helpful chat companion on Pulse Messenger."
        }
    }

    private fun getOfflineFallback(username: String, lastUserMessage: String): String {
        val lowercaseMsg = lastUserMessage.lowercase()
        return when (username) {
            "pidor" -> {
                if (lowercaseMsg.contains("привіт") || lowercaseMsg.contains("здоров") || lowercaseMsg.contains("hi") || lowercaseMsg.contains("hello")) {
                    "Здоров! Як справи? Радий бачити тебе в Pulse. Тут все літає і повністю зашифровано!"
                } else if (lowercaseMsg.contains("шифр") || lowercaseMsg.contains("крипт") || lowercaseMsg.contains("secure") || lowercaseMsg.contains("e2ee")) {
                    "Так, брате, це надійний AES-128! База даних Room зберігає тільки зашифровані байти. Ніяких витоків!"
                } else {
                    "Хах, згоден! До речі, мій нік @pidor — це чисто локальний мем для тестування юзернеймів, ТЗ виконали на 100% 😂."
                }
            }
            "alex" -> {
                if (lowercaseMsg.contains("hi") || lowercaseMsg.contains("hello") || lowercaseMsg.contains("привіт")) {
                    "Hi there! Welcome to Pulse. I'm currently tweaking the Room database index optimizations to handle heavy load."
                } else {
                    "That's interesting! By the way, check out your profile settings to switch between Online, Idle, and DND statuses. It works instantly like Discord."
                }
            }
            "julia" -> {
                "Hey! Do you like the Obsidian Dark theme of Pulse? I spent hours ensuring proper spacing, nice rounded cards, and proper contrast ratios."
            }
            "pulse_bot" -> {
                "Hello! I am Pulse AI Assistant. I can help you test out our ultra-fast messaging client. Feel free to toggle the 'Show Encryption' option inside chats to see the raw cipher stored on SQLite!"
            }
            else -> "Hello! Glad to chat with you on Pulse."
        }
    }
}
