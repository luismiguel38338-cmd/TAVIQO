package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.UiState
import com.example.model.GiftCatalogItem
import com.example.model.LiveChatMessage
import com.example.model.LiveStream
import com.example.ui.components.SendGiftBottomSheet
import com.example.ui.components.VideoPlayerBox
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DeepBlack
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
fun LiveStreamsScreen(
    currentUserId: String,
    streamsState: UiState<List<LiveStream>>,
    blockedUserIds: Set<String>,
    coinBalance: Long,
    observeChat: (String) -> Flow<List<LiveChatMessage>>,
    onStartLiveStream: (title: String, category: String, streamUrl: String, onCreated: (String) -> Unit) -> Unit,
    onEndLiveStream: (String) -> Unit,
    onJoinStream: (String) -> Unit,
    onLeaveStream: (String) -> Unit,
    onSendChat: (String, String) -> Unit,
    onSendGift: (LiveStream, GiftCatalogItem) -> Unit,
    onBlockHost: (String) -> Unit,
    onReportStream: (LiveStream, String, String) -> Unit,
    onOpenWallet: () -> Unit
) {
    var activeStreamId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val activeStreams = (streamsState as? UiState.Success)?.data
        ?.filter { it.hostId !in blockedUserIds }
        ?: emptyList()
    val currentStream = activeStreams.firstOrNull { it.id == activeStreamId }

    if (currentStream != null) {
        LiveRoomView(
            stream = currentStream,
            currentUserId = currentUserId,
            coinBalance = coinBalance,
            chatFlow = observeChat(currentStream.id),
            onJoin = { onJoinStream(currentStream.id) },
            onLeave = {
                onLeaveStream(currentStream.id)
                activeStreamId = null
            },
            onEndStream = {
                onEndLiveStream(currentStream.id)
                activeStreamId = null
            },
            onSendChat = { text -> onSendChat(currentStream.id, text) },
            onSendGift = { gift -> onSendGift(currentStream, gift) },
            onBlockHost = {
                onBlockHost(currentStream.hostId)
                activeStreamId = null
            },
            onReportStream = { reason, details -> onReportStream(currentStream, reason, details) },
            onOpenWallet = onOpenWallet
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
                Column {
                    Text("En Vivo", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        "Transmisiones reales con chat y regalos instantáneos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = TextPrimary),
                    modifier = Modifier.testTag("start_live_button")
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Iniciar Directo")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (streamsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
                is UiState.Error -> {
                    Text(streamsState.message, color = LiveCrimson)
                }
                is UiState.Success -> {
                    if (activeStreams.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, RoyalBlueBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_live_stage),
                                    contentDescription = "Escenario en vivo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No hay transmisiones activas ahora mismo",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Inicia tu propio directo real con URL HLS/RTMP o stream configurado para que la comunidad entre al chat y envíe regalos.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(activeStreams, key = { it.id }) { stream ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = RoyalBlueDark,
                                    border = BorderStroke(1.dp, RoyalBlueBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeStreamId = stream.id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(RoyalBlueElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = LiveCrimson, modifier = Modifier.size(28.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(color = LiveCrimson, shape = RoundedCornerShape(6.dp)) {
                                                    Text(
                                                        "EN VIVO",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stream.category, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(stream.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                            Text(
                                                "@${stream.hostUsername} • ${stream.viewerCount} espectadores • ${stream.giftsCoinsTotal} monedas",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        StartLiveDialog(
            onStart = { title, category, url ->
                onStartLiveStream(title, category, url) { newId ->
                    showCreateDialog = false
                    activeStreamId = newId
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun LiveRoomView(
    stream: LiveStream,
    currentUserId: String,
    coinBalance: Long,
    chatFlow: Flow<List<LiveChatMessage>>,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onEndStream: () -> Unit,
    onSendChat: (String) -> Unit,
    onSendGift: (GiftCatalogItem) -> Unit,
    onBlockHost: () -> Unit,
    onReportStream: (String, String) -> Unit,
    onOpenWallet: () -> Unit
) {
    val chatMessages by chatFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var chatInput by remember { mutableStateOf("") }
    var showGiftSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    DisposableEffect(stream.id) {
        onJoin()
        onDispose { onLeave() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_live_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        VideoPlayerBox(
            videoUrl = stream.streamUrl,
            isActive = true,
            isMuted = false,
            showTapPause = false,
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLeave,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DeepBlack.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir del directo", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DeepBlack.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, RoyalBlueBorder)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("@${stream.hostUsername}", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                        Text(stream.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = LiveCrimson) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${stream.viewerCount}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }

                if (stream.hostId == currentUserId) {
                    IconButton(
                        onClick = onEndStream,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LiveCrimson)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = "Finalizar directo", tint = TextPrimary)
                    }
                } else {
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DeepBlack.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = "Denunciar directo", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = onBlockHost,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DeepBlack.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "Bloquear anfitrión", tint = LiveCrimson)
                    }
                }
            }
        }

        // Bottom live chat + input + gift button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    val isGift = msg.kind == "gift"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isGift) RoyalBlueElevated.copy(alpha = 0.9f) else DeepBlack.copy(alpha = 0.58f),
                        border = if (isGift) BorderStroke(1.dp, CoinGold) else null
                    ) {
                        Text(
                            text = "@${msg.senderUsername}: ${msg.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isGift) FontWeight.Bold else FontWeight.Normal,
                            color = if (isGift) CoinGold else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Comenta en vivo...", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = RoyalBlueBorder,
                        focusedContainerColor = DeepBlack.copy(alpha = 0.7f),
                        unfocusedContainerColor = DeepBlack.copy(alpha = 0.7f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            onSendChat(chatInput)
                            chatInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(NeonCyan, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar chat", tint = DeepBlack)
                }
                if (stream.hostId != currentUserId) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showGiftSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(CoinGold, CircleShape)
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = "Enviar regalo en vivo", tint = DeepBlack)
                    }
                }
            }
        }
    }

    if (showGiftSheet) {
        SendGiftBottomSheet(
            recipientUsername = stream.hostUsername,
            coinBalance = coinBalance,
            onSelectGift = onSendGift,
            onOpenWallet = onOpenWallet,
            onDismiss = { showGiftSheet = false }
        )
    }

    if (showReportDialog) {
        ReportDialog(
            title = "Denunciar directo de @${stream.hostUsername}",
            onSubmit = { reason, details ->
                onReportStream(reason, details)
                showReportDialog = false
            },
            onDismiss = { showReportDialog = false }
        )
    }
}

@Composable
private fun StartLiveDialog(
    onStart: (title: String, category: String, streamUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Música y Charla") }
    var streamUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Iniciar Transmisión en Vivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del directo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    label = { Text("URL real del stream (HLS .m3u8 / MP4)") },
                    placeholder = { Text("https://...", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(title, category, streamUrl) },
                enabled = title.trim().length >= 2 && streamUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = TextPrimary)
            ) {
                Text("Emitir ahora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
