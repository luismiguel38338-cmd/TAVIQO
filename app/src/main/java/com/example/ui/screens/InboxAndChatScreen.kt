package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.UiState
import com.example.model.Conversation
import com.example.model.DirectMessage
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.example.ui.components.TaviqoLogoMark
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow

@Composable
fun InboxAndChatScreen(
    currentUserId: String,
    creators: List<UserProfile>,
    blockedUserIds: Set<String>,
    conversationsState: UiState<List<Conversation>>,
    notificationsState: UiState<List<UserNotification>>,
    observeMessages: (String) -> Flow<List<DirectMessage>>,
    onOpenOrCreateConversation: (UserProfile, onReady: (String) -> Unit) -> Unit,
    onSendDirectMessage: (Conversation, String, String?) -> Unit,
    onMarkConversationRead: (Conversation, List<DirectMessage>) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit
) {
    var activeConvId by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val activeConv = conversations.firstOrNull { it.id == activeConvId }

    if (activeConv != null) {
        BackHandler { activeConvId = null }
        DirectChatThread(
            currentUserId = currentUserId,
            conversation = activeConv,
            blockedUserIds = blockedUserIds,
            messagesFlow = observeMessages(activeConv.id),
            onBack = { activeConvId = null },
            onSendMessage = { text, imageUrl -> onSendDirectMessage(activeConv, text, imageUrl) },
            onMarkRead = { msgs -> onMarkConversationRead(activeConv, msgs) },
            onToggleBlock = onToggleBlock
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TaviqoLogoMark(size = 30.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Mensajes TAVIQO", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                }
                IconButton(
                    onClick = { showNewChatDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RoyalBlueElevated)
                        .testTag("new_chat_button")
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "Nuevo mensaje", tint = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianSurface,
                contentColor = NeonCyan,
                indicator = { positions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mensajes (${conversations.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val unread = notifications.count { !it.read }
                        Text("Notificaciones ($unread)")
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                val visibleConvs = conversations.filter { conv ->
                    val otherId = conv.participantIds.firstOrNull { it != currentUserId }
                    otherId == null || otherId !in blockedUserIds
                }
                if (visibleConvs.isEmpty()) {
                    Text(
                        text = "No tienes conversaciones activas. Toca + para enviar un mensaje privado a otro usuario real.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visibleConvs, key = { it.id }) { conv ->
                            val otherId = conv.participantIds.firstOrNull { it != currentUserId }.orEmpty()
                            val otherName = conv.participantNames[otherId] ?: "usuario"
                            val hasUnread = currentUserId in conv.unreadUserIds
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (hasUnread) RoyalBlueElevated else RoyalBlueDark,
                                border = BorderStroke(1.dp, if (hasUnread) NeonCyan else RoyalBlueBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeConvId = conv.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("@$otherName", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(
                                            text = conv.lastMessage.ifBlank { "Conversación iniciada" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (hasUnread) NeonCyan else TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    if (hasUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(NeonCyan)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (notifications.isEmpty()) {
                    Text(
                        text = "Sin notificaciones nuevas todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(notifications, key = { it.id }) { notif ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (!notif.read) RoyalBlueElevated else RoyalBlueDark,
                                border = BorderStroke(1.dp, if (!notif.read) NeonCyan else RoyalBlueBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMarkNotificationRead(notif.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (notif.read) Icons.Default.MarkEmailRead else Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (notif.read) TextMuted else NeonCyan
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(notif.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(notif.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        val availableUsers = creators.filter { it.userId != currentUserId && it.userId !in blockedUserIds }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            title = { Text("Iniciar chat privado") },
            text = {
                if (availableUsers.isEmpty()) {
                    Text("No hay otros usuarios disponibles todavía.", color = TextSecondary)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableUsers, key = { it.userId }) { user ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RoyalBlueDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenOrCreateConversation(user) { convId ->
                                            showNewChatDialog = false
                                            activeConvId = convId
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cerrar", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
private fun DirectChatThread(
    currentUserId: String,
    conversation: Conversation,
    blockedUserIds: Set<String>,
    messagesFlow: Flow<List<DirectMessage>>,
    onBack: () -> Unit,
    onSendMessage: (text: String, imageUrl: String?) -> Unit,
    onMarkRead: (List<DirectMessage>) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit
) {
    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }

    val otherId = conversation.participantIds.firstOrNull { it != currentUserId }.orEmpty()
    val otherName = conversation.participantNames[otherId] ?: "usuario"
    val isBlocked = otherId in blockedUserIds

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.any { currentUserId !in it.readBy } || currentUserId in conversation.unreadUserIds) {
            onMarkRead(messages)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Text("@$otherName", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
            }
            IconButton(
                onClick = { onToggleBlock(otherId, !isBlocked) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = if (isBlocked) "Desbloquear" else "Bloquear",
                    tint = if (isBlocked) EmeraldSuccess else LiveCrimson
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                val seenByOther = otherId in msg.readBy
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isMine) RoyalBlueElevated else RoyalBlueDark,
                        border = BorderStroke(1.dp, if (isMine) NeonCyan.copy(alpha = 0.5f) else RoyalBlueBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (msg.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = msg.imageUrl,
                                    contentDescription = "Imagen enviada",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            if (isMine) {
                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Estado de lectura",
                                        tint = if (seenByOther) NeonCyan else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!attachedImageUri.isNullOrBlank()) {
            Text(
                text = "Imagen adjunta lista para enviar",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = "Adjuntar imagen", tint = NeonCyan)
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Mensaje privado...", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = RoyalBlueBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || !attachedImageUri.isNullOrBlank()) {
                        onSendMessage(inputText, attachedImageUri)
                        inputText = ""
                        attachedImageUri = null
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(NeonCyan, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar mensaje", tint = DeepBlack)
            }
        }
    }
}
