package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: PulseViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }

    // Text inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    // Password visibility state
    var passwordVisible by remember { mutableStateOf(false) }

    // Loading and error state
    var isLoading by remember { mutableStateOf(false) }

    // Database configuration dialog state
    var showUrlConfigDialog by remember { mutableStateOf(false) }
    var dbUrlInput by remember { mutableStateOf(viewModel.getFirebaseDbUrl(context)) }
    var apiKeyInput by remember { mutableStateOf(viewModel.getFirebaseApiKey(context)) }
    var projectIdInput by remember { mutableStateOf(viewModel.getFirebaseProjectId(context)) }
    var appIdInput by remember { mutableStateOf(viewModel.getFirebaseAppId(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberObsidian)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Settings Cog for Database URL configuration
        IconButton(
            onClick = { showUrlConfigDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configure Database",
                tint = PulseCyan
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulse App Logo
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Pulse",
                tint = PulseCyan,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PulseCyan.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PULSE MESSENGER",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp
            )

            Text(
                text = "Secure, real-time communication",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Auth Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Switcher (Tab-like row)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberObsidian)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isRegisterMode) PulseIndigo else Color.Transparent)
                                .clickable { isRegisterMode = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRegisterMode) PulseIndigo else Color.Transparent)
                                .clickable { isRegisterMode = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Register",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fields
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PulseCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = PulseCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username (e.g. pidor, alex)") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = PulseCyan) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PulseCyan,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                focusedLabelColor = PulseCyan,
                                unfocusedLabelColor = TextSecondary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PulseCyan) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PulseCyan,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                focusedLabelColor = PulseCyan,
                                unfocusedLabelColor = TextSecondary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PulseCyan) },
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = "Toggle password visibility", tint = TextSecondary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = PulseCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = PulseCyan)
                    } else {
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(context, "Please fill in email and password", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (isRegisterMode && username.isBlank()) {
                                    Toast.makeText(context, "Please choose a username", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true
                                if (isRegisterMode) {
                                    viewModel.registerUser(
                                        email = email,
                                        password = password,
                                        username = username,
                                        displayName = displayName,
                                        onSuccess = {
                                            isLoading = false
                                            Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    viewModel.loginUser(
                                        email = email,
                                        password = password,
                                        onSuccess = {
                                            isLoading = false
                                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(PulseCyan, PulseIndigo))),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text(
                                text = if (isRegisterMode) "REGISTER NOW" else "SIGN IN",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dynamic configuration dialog for custom Firebase Database & Auth config
    if (showUrlConfigDialog) {
        Dialog(onDismissRequest = { showUrlConfigDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Firebase Configuration",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Customize your own Firebase project configuration parameters below.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    OutlinedTextField(
                        value = dbUrlInput,
                        onValueChange = { dbUrlInput = it },
                        label = { Text("Database URL", fontSize = 12.sp) },
                        placeholder = { Text("https://example.firebaseio.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = PulseCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key", fontSize = 12.sp) },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = PulseCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = projectIdInput,
                        onValueChange = { projectIdInput = it },
                        label = { Text("Project ID", fontSize = 12.sp) },
                        placeholder = { Text("my-firebase-project") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = PulseCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = appIdInput,
                        onValueChange = { appIdInput = it },
                        label = { Text("Application ID (App ID)", fontSize = 12.sp) },
                        placeholder = { Text("1:1234567:android:abcd") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = PulseCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showUrlConfigDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateFirebaseConfig(
                                    context = context,
                                    url = dbUrlInput,
                                    apiKey = apiKeyInput,
                                    projectId = projectIdInput,
                                    appId = appIdInput
                                )
                                showUrlConfigDialog = false
                                Toast.makeText(context, "Firebase custom configuration saved", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseCyan)
                        ) {
                            Text("Save Config", color = CyberObsidian)
                        }
                    }
                }
            }
        }
    }
}
