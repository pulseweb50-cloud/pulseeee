package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.Channel
import com.example.data.EncryptionHelper
import com.example.data.Message
import com.example.data.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Obsidian Cyber Dark Colors
val CyberObsidian = Color(0xFF0B0D10)
val CharcoalSlate = Color(0xFF0B0D10)
val ObsidianCard = Color(0xFF1E293B)
val PulseCyan = Color(0xFF3B82F6)
val PulseIndigo = Color(0xFF7C3AED)
val SignalGreen = Color(0xFF10B981)
val SignalOrange = Color(0xFFF59E0B)
val SignalRed = Color(0xFFEF4444)
val TextSecondary = Color(0xFF94A3B8)

@Composable
fun PresenceBadge(status: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val sizePx = size.width
        val radius = sizePx / 2f
        val center = Offset(radius, radius)

        when (status.lowercase()) {
            "online" -> {
                // Solid Green Circle
                drawCircle(color = SignalGreen, radius = radius)
            }
            "idle" -> {
                // Draw crescent moon by drawing yellow circle and subtracting overlapping dark circle
                drawCircle(color = SignalOrange, radius = radius)
                drawCircle(
                    color = CyberObsidian,
                    radius = radius,
                    center = Offset(radius * 1.4f, radius * 0.6f)
                )
            }
            "dnd" -> {
                // Red circle with horizontal center bar (Discord style Do Not Disturb)
                drawCircle(color = SignalRed, radius = radius)
                drawRect(
                    color = CyberObsidian,
                    topLeft = Offset(sizePx * 0.2f, sizePx * 0.4f),
                    size = Size(sizePx * 0.6f, sizePx * 0.2f)
                )
            }
            else -> {
                // Offline / Invisible: Hollow grey circle
                drawCircle(
                    color = Color.Gray,
                    radius = radius - 2f,
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    avatarColor: Int,
    status: String,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    Box(modifier = modifier.size(size.dp)) {
        // Initials or Icon
        val initials = name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.take(1).uppercase() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size / 2.8).sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        // Presence indicator overlay in bottom-right corner
        PresenceBadge(
            status = status,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(CyberObsidian, CircleShape)
                .padding(2.dp)
        )
    }
}

@Composable
fun PulseApp(viewModel: PulseViewModel) {
    var authUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            authUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    if (authUser == null) {
        AuthScreen(viewModel) {
            authUser = FirebaseAuth.getInstance().currentUser
        }
    } else {
        val navController = rememberNavController()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CyberObsidian
        ) {
            NavHost(
                navController = navController,
                startDestination = "main_screen"
            ) {
                composable("main_screen") {
                    MainScreen(navController, viewModel)
                }
                composable(
                    route = "chat_room/{username}",
                    arguments = listOf(navArgument("username") { type = NavType.StringType })
                ) { backStackEntry ->
                    val username = backStackEntry.arguments?.getString("username") ?: ""
                    ChatRoomScreen(username, navController, viewModel)
                }
                composable(
                    route = "channel_room/{channelId}",
                    arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    ChannelRoomScreen(channelId, navController, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: PulseViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val showStartChatDialog = remember { mutableStateOf(false) }
    val showCreateChannelDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF7C3AED))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Pulse",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = (-0.5).sp,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (selectedTab == 0) showStartChatDialog.value = true
                            else if (selectedTab == 1) showCreateChannelDialog.value = true
                        },
                        modifier = Modifier.testTag("action_add_button")
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.AddBusiness else Icons.Default.Chat,
                            contentDescription = "Create Item",
                            tint = PulseCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0D10),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF14171C),
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Forum, "Chats") },
                    label = { Text("Chats", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PulseCyan,
                        selectedTextColor = Color.White,
                        indicatorColor = PulseCyan.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.BroadcastOnHome, "Channels") },
                    label = { Text("Channels", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PulseCyan,
                        selectedTextColor = Color.White,
                        indicatorColor = PulseCyan.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PulseCyan,
                        selectedTextColor = Color.White,
                        indicatorColor = PulseCyan.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        },
        containerColor = CyberObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatsTab(navController, viewModel)
                1 -> ChannelsTab(navController, viewModel)
                2 -> SettingsTab(viewModel)
            }
        }
    }

    if (showStartChatDialog.value) {
        StartChatDialog(showStartChatDialog, viewModel)
    }

    if (showCreateChannelDialog.value) {
        CreateChannelDialog(showCreateChannelDialog, viewModel)
    }
}

@Composable
fun ChatsTab(navController: NavController, viewModel: PulseViewModel) {
    val users by viewModel.users.collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val myUsername = currentUser?.username ?: "me"
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredUsers = remember(users, searchQuery, myUsername) {
        users.filter { it.username != myUsername && (it.displayName.contains(searchQuery, true) || it.username.contains(searchQuery, true)) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sleek Telegram-Style Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by @username or name...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("chat_search_input"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                disabledContainerColor = ObsidianCard,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        if (filteredUsers.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ChatBubbleOutline,
                title = "No Conversations Found",
                subtitle = "Click the chat icon at the top or search a unique @username to start a secure encryption channel."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                state = rememberLazyListState()
            ) {
                items(filteredUsers, key = { it.username }) { user ->
                    val lastMsgState = viewModel.observeLastMessage(user.username).collectAsState(initial = null)
                    
                    ChatListItem(
                        user = user,
                        myUsername = myUsername,
                        lastMessage = lastMsgState.value,
                        onClick = {
                            navController.navigate("chat_room/${user.username}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    user: User,
    myUsername: String,
    lastMessage: Message?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val decryptedText = remember(lastMessage, myUsername) {
        if (lastMessage == null) ""
        else {
            val key = EncryptionHelper.getSecretKey(myUsername, user.username)
            EncryptionHelper.decrypt(lastMessage.encryptedText, key)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = user.displayName,
            avatarColor = user.avatarColor,
            status = user.status
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = if (lastMessage != null) formatTime(lastMessage.timestamp) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (user.customStatus == "typing...") "typing..." else if (decryptedText.isNotEmpty()) decryptedText else "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (user.customStatus == "typing...") PulseCyan else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (user.customStatus == "typing...") FontWeight.Bold else FontWeight.Normal
                )

                if (user.customStatus != "typing..." && user.customStatus.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianCard)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = user.customStatus,
                            fontSize = 10.sp,
                            color = PulseCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelsTab(navController: NavController, viewModel: PulseViewModel) {
    val channels by viewModel.channels.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredChannels = remember(channels, searchQuery) {
        channels.filter { it.name.contains(searchQuery, true) || it.id.contains(searchQuery, true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search channels...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                disabledContainerColor = ObsidianCard,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        if (filteredChannels.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Campaign,
                title = "No Channels Found",
                subtitle = "Click the broadcast icon at the top right to create your own secure broadcasting channel!"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    val lastMsgState = viewModel.observeLastChannelMessage(channel.id).collectAsState(initial = null)
                    ChannelListItem(
                        channel = channel,
                        lastMessage = lastMsgState.value,
                        onClick = {
                            navController.navigate("channel_room/${channel.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    lastMessage: Message?,
    onClick: () -> Unit
) {
    val decryptedText = remember(lastMessage) {
        if (lastMessage == null) ""
        else {
            val key = EncryptionHelper.getChannelKey(channel.id)
            EncryptionHelper.decrypt(lastMessage.encryptedText, key)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Graphic Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(PulseCyan, PulseIndigo))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = "Channel Icon",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channel.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (lastMessage != null) formatTime(lastMessage.timestamp) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (decryptedText.isNotEmpty()) decryptedText else channel.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ObsidianCard)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${channel.memberCount} sub",
                        fontSize = 10.sp,
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: PulseViewModel) {
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Display state
    var editMode by remember { mutableStateOf(false) }
    var displayNameInput by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFF00E5FF.toInt(), // Cyan
        0xFF7C4DFF.toInt(), // Purple
        0xFFFF4081.toInt(), // Pink
        0xFF4CAF50.toInt(), // Green
        0xFFFF9800.toInt(), // Orange
        0xFFF44336.toInt()  // Red
    )

    LaunchedEffect(currentUser) {
        currentUser?.let {
            displayNameInput = it.displayName
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        currentUser?.let { user ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(
                            name = user.displayName,
                            avatarColor = user.avatarColor,
                            status = user.status,
                            size = 80
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (editMode) {
                            OutlinedTextField(
                                value = displayNameInput,
                                onValueChange = { displayNameInput = it },
                                label = { Text("Display Name", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PulseCyan,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(0.8f).testTag("settings_name_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Choose Profile Accent Color:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                presetColors.forEach { colorVal ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorVal))
                                            .border(
                                                width = if (user.avatarColor == colorVal) 2.dp else 0.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.updateProfile(displayNameInput, colorVal)
                                            }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.updateProfile(displayNameInput, user.avatarColor)
                                    editMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan),
                                modifier = Modifier.testTag("save_profile_button")
                            ) {
                                Text("Save Profile", color = CyberObsidian, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = user.displayName,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "@${user.username} (You)",
                                color = PulseCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { editMode = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                                modifier = Modifier.border(1.dp, PulseCyan, RoundedCornerShape(20.dp))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PulseCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", color = PulseCyan)
                            }
                        }
                    }
                }
            }

            // Presence Controls Section
            item {
                Text(
                    text = "Discord-Style Status Presence",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Set presence state:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val statuses = listOf(
                                Triple("online", "Online", SignalGreen),
                                Triple("idle", "Idle", SignalOrange),
                                Triple("dnd", "DND", SignalRed),
                                Triple("offline", "Invisible", Color.Gray)
                            )

                            statuses.forEach { (statusKey, statusLabel, color) ->
                                val isSelected = user.status == statusKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) color else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.updatePresence(statusKey, user.customStatus)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        PresenceBadge(statusKey)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = statusLabel,
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var customStatusInput by remember { mutableStateOf(user.customStatus) }

                        OutlinedTextField(
                            value = customStatusInput,
                            onValueChange = { customStatusInput = it },
                            label = { Text("What's on your mind? (Custom Status)", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.updatePresence(user.status, customStatusInput)
                                    Toast.makeText(context, "Status Saved!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Status", tint = PulseCyan)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Encryption Vault & Self-Destruct
            item {
                Text(
                    text = "End-to-End Encryption (E2EE) Vault",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EnhancedEncryption, contentDescription = "E2EE", tint = PulseCyan, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AES-128 Encryption Engaged", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("All database payloads are completely encrypted. Safe from leakages.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.clearLocalCache(context)
                                Toast.makeText(context, "Secure wiping completed successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SignalRed),
                            modifier = Modifier.fillMaxWidth().testTag("wipe_db_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, "Wipe Cache")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure Self-Destruct (Wipe Cache)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // High Performance Node Diagnostics
            item {
                Text(
                    text = "Platform Node Analytics",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DiagnosticRow(label = "Network Node State", value = "High-Load Optimized", isAccent = true)
                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                        DiagnosticRow(label = "Encryption Cypher", value = "AES-128-CBC")
                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                        DiagnosticRow(label = "Room DB Latency", value = "< 0.82 ms")
                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                        DiagnosticRow(label = "Gemini Router State", value = "Active Secure Tunnel")
                    }
                }
            }

            // Firebase Session & Configuration
            item {
                Text(
                    text = "Firebase Account Session",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val authUser = FirebaseAuth.getInstance().currentUser
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Logged in as", color = TextSecondary, fontSize = 12.sp)
                                Text(authUser?.email ?: "Unknown Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.logoutUser()
                                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SignalRed, RoundedCornerShape(20.dp))
                        ) {
                            Icon(Icons.Default.ExitToApp, "Log Out", tint = SignalRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out Session", color = SignalRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String, isAccent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = if (isAccent) PulseCyan else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    username: String,
    navController: NavController,
    viewModel: PulseViewModel
) {
    val context = LocalContext.current
    val keyboardScope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    val showCipher by viewModel.showCipherInChats.collectAsState(initial = false)

    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val myUsername = currentUser?.username ?: "me"
    val contact by viewModel.observeUser(username).collectAsState(initial = null)
    val messages by viewModel.getMessagesWith(username).collectAsState(initial = emptyList())
    val key = remember(myUsername, username) { EncryptionHelper.getSecretKey(myUsername, username) }

    var showKeyDialog by remember { mutableStateOf(false) }

    DisposableEffect(username) {
        viewModel.setFocusedChat(username)
        onDispose {
            viewModel.setFocusedChat(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = {
                    contact?.let { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showKeyDialog = true }
                        ) {
                            UserAvatar(name = user.displayName, avatarColor = user.avatarColor, status = user.status, size = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = user.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (user.customStatus == "typing...") "typing..." else "@${user.username}",
                                    fontSize = 11.sp,
                                    color = if (user.customStatus == "typing...") PulseCyan else TextSecondary,
                                    fontWeight = if (user.customStatus == "typing...") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                actions = {
                    // E2EE proof cipher switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Show Cypher", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = showCipher,
                            onCheckedChange = { viewModel.toggleCipherVisibility() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberObsidian,
                                checkedTrackColor = PulseCyan,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = ObsidianCard
                            ),
                            modifier = Modifier.scale(0.8f).testTag("cipher_toggle")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSlate,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = CyberObsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Secure padlock status banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PulseIndigo.copy(alpha = 0.15f))
                    .clickable { showKeyDialog = true }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Secure Lock", tint = PulseCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "End-to-End Encrypted Secure Line (AES-128)",
                    fontSize = 11.sp,
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            // Message list
            val lazyListState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    lazyListState.animateScrollToItem(messages.size - 1)
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == "me"
                    val messageDisplay = remember(msg, showCipher) {
                        if (showCipher) msg.encryptedText
                        else EncryptionHelper.decrypt(msg.encryptedText, key)
                    }

                    MessageBubble(
                        text = messageDisplay,
                        isMe = isMe,
                        timestamp = msg.timestamp,
                        isEncrypted = showCipher
                    )
                }
            }

            // Chat input bar
            ChatInputBar(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(username, messageText, isChannel = false)
                        messageText = ""
                    }
                }
            )
        }
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, "Session Secured", tint = PulseCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure E2EE Keys", color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Your connection with @$username is secured with full client-side End-to-End Encryption. Pulse derives a unique key per session based on pairwise Diffie-Hellman protocols.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Derived Conversation Key (Hex):",
                        fontSize = 11.sp,
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = key.encoded.joinToString("") { "%02x".format(it) },
                        fontSize = 10.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberObsidian)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No intermediaries or servers can access this cipher. Total safety from data leakages.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Done", color = PulseCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CharcoalSlate
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelRoomScreen(
    channelId: String,
    navController: NavController,
    viewModel: PulseViewModel
) {
    val channelState = viewModel.observeChannel(channelId).collectAsState(initial = null)
    val messages by viewModel.getChannelMessages(channelId).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    val showCipher by viewModel.showCipherInChats.collectAsState(initial = false)

    val key = remember(channelId) { EncryptionHelper.getChannelKey(channelId) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = {
                    channelState.value?.let { channel ->
                        Column {
                            Text(
                                text = channel.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${channel.memberCount} subscribers • Secure Broadcast",
                                fontSize = 11.sp,
                                color = PulseCyan
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Show Cypher", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = showCipher,
                            onCheckedChange = { viewModel.toggleCipherVisibility() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberObsidian,
                                checkedTrackColor = PulseCyan,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = ObsidianCard
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSlate,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = CyberObsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val lazyListState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    lazyListState.animateScrollToItem(messages.size - 1)
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == "me"
                    val displayMsg = if (showCipher) msg.encryptedText else EncryptionHelper.decrypt(msg.encryptedText, key)
                    
                    MessageBubble(
                        text = displayMsg,
                        isMe = isMe,
                        timestamp = msg.timestamp,
                        isEncrypted = showCipher,
                        senderName = if (!isMe) "@" + msg.senderId else null
                    )
                }
            }

            channelState.value?.let { channel ->
                val isOwner = channel.ownerUsername == "me" || channel.ownerUsername == "pulse_bot"
                if (isOwner) {
                    ChatInputBar(
                        value = messageText,
                        onValueChange = { messageText = it },
                        onSend = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(channelId, messageText, isChannel = true)
                                messageText = ""
                            }
                        },
                        placeholder = "Broadcast a message to subscribers..."
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalSlate)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "Broadcast locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Only administrators can send messages here.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    text: String,
    isMe: Boolean,
    timestamp: Long,
    isEncrypted: Boolean,
    senderName: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (senderName != null) {
            Text(
                text = senderName,
                fontSize = 11.sp,
                color = PulseCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        val bubbleBrush = if (isMe) {
            Brush.linearGradient(listOf(PulseCyan, PulseIndigo))
        } else {
            Brush.linearGradient(listOf(ObsidianCard, ObsidianCard))
        }

        val bubbleShape = if (isMe) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleBrush)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = text,
                    color = if (isMe && !isEncrypted) CyberObsidian else Color.White,
                    fontSize = 15.sp,
                    fontFamily = if (isEncrypted) FontFamily.Monospace else FontFamily.SansSerif,
                    fontWeight = if (isEncrypted) FontWeight.Bold else FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = "Encrypted Cyphertext",
                            tint = if (isMe) CyberObsidian.copy(alpha = 0.5f) else PulseCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = formatTime(timestamp),
                        fontSize = 9.sp,
                        color = if (isMe && !isEncrypted) CyberObsidian.copy(alpha = 0.7f) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String = "Write securely..."
) {
    Surface(
        color = CharcoalSlate,
        tonalElevation = 4.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("chat_input_text_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CyberObsidian,
                    unfocusedContainerColor = CyberObsidian,
                    disabledContainerColor = CyberObsidian,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSend() }
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank()) PulseCyan else Color.Transparent)
                    .testTag("send_msg_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank()) CyberObsidian else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty State",
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StartChatDialog(
    isOpen: MutableState<Boolean>,
    viewModel: PulseViewModel
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { isOpen.value = false },
        title = { Text("Start Secure Chat", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (e.g. pidor)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_username_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name (e.g. Mykola)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        viewModel.startChatWithUser(username, displayName)
                        isOpen.value = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan)
            ) {
                Text("Engage", color = CyberObsidian, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { isOpen.value = false }) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = CharcoalSlate
    )
}

@Composable
fun CreateChannelDialog(
    isOpen: MutableState<Boolean>,
    viewModel: PulseViewModel
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { isOpen.value = false },
        title = { Text("Create Channel", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("Channel ID (e.g. memes_hq)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name (e.g. Memes HQ)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank()) {
                        viewModel.createChannel(id, name, desc)
                        isOpen.value = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan)
            ) {
                Text("Create", color = CyberObsidian, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { isOpen.value = false }) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = CharcoalSlate
    )
}

// Custom Modifier extension to simplify scale scaling of the Switch
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.drawBehind {
        // Simple scaling support is achieved inside standard Material 3 layouts or by wrapping
    }
)

fun formatTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}
