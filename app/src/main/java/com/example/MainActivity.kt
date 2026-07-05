package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.Repository
import com.example.ui.PulseApp
import com.example.ui.PulseViewModel
import com.example.ui.PulseViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.NotificationHelper
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge full screen drawing
        enableEdgeToEdge()

        // Initialize Firebase from app/google-services.json (auto-parsed by the
        // google-services Gradle plugin at build time). That file is gitignored on
        // purpose - see README "Firebase setup" - so no real project keys ever live
        // in source code. If the user has saved a custom Firebase project in
        // Settings ("Firebase Account Session"), reuse those saved values instead.
        try {
            val prefs = getSharedPreferences("pulse_prefs", MODE_PRIVATE)
            val dbUrl = prefs.getString("firebase_db_url", "")?.trim()?.ifEmpty { null }
            val apiKey = prefs.getString("firebase_api_key", "")?.trim()?.ifEmpty { null }
            val projectId = prefs.getString("firebase_project_id", "")?.trim()?.ifEmpty { null }
            val appId = prefs.getString("firebase_app_id", "")?.trim()?.ifEmpty { null }

            if (apiKey != null && projectId != null && appId != null && dbUrl != null) {
                // A custom Firebase project was configured by the user - use it.
                try {
                    FirebaseApp.getInstance().delete()
                } catch (e: Exception) {
                    // No default instance yet, nothing to delete.
                }
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .setDatabaseUrl(dbUrl)
                    .build()
                FirebaseApp.initializeApp(applicationContext, options)
            } else {
                // Default path: read config from google-services.json.
                FirebaseApp.initializeApp(applicationContext)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize local push notifications channel
        NotificationHelper.createNotificationChannel(applicationContext)

        // Initialize database with lifecycle Scope
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = Repository(applicationContext, database, lifecycleScope)
        
        // Spin up the ViewModel using its custom factory
        val viewModel = ViewModelProvider(
            this, 
            PulseViewModelFactory(repository)
        )[PulseViewModel::class.java]

        setContent {
            MyApplicationTheme {
                PulseApp(viewModel)
            }
        }
    }
}
