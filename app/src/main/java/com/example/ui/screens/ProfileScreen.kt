package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Clip
import com.example.model.GiftCatalogItem
import com.example.model.UserProfile
import com.example.ui.components.SendGiftBottomSheet
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

@Composable
fun ProfileScreen(
    currentUserId: String,
    viewedProfile: UserProfile?,
    allClips: List<Clip>,
    favoriteIds: Set<String>,
    followingIds: Set<String>,
    blockedUserIds: Set<String>,
    coinBalance: Long,
    isAdmin: Boolean,
    isOwnProfile: Boolean,
    onBack: (() -> Unit)?,
    onEditProfile: (username: String, displayName: String, bio: String, avatarUrl: String?) -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onSendGift: (UserProfile, GiftCatalogItem) -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showGiftSheet by remember { mutableStateOf(false) }

    val profile = viewedProfile
    val userClips = remember(allClips, profile) {
        if (profile == null) emptyList() else allClips.filter { it.authorId == profile.userId }
    }
    val favoriteClips = remember(allClips, favoriteIds) {
        allClips.filter { it.id in favoriteIds }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                        }
                    }
                    Text(
                        text = if (isOwnProfile) "Mi Perfil" else "@${profile?.username ?: ""}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }

                if (isOwnProfile) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onOpenAiAssistant,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RoyalBlueElevated)
                                .testTag("open_ai_assistant_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Asistente Gemini AI", tint = NeonCyan)
                        }
                        if (isAdmin) {
                            IconButton(
                                onClick = onOpenAdminPanel,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(RoyalBlueElevated)
                                    .testTag("open_admin_panel_button")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Panel Admin", tint = CoinGold)
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RoyalBlueDark)
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", tint = LiveCrimson)
                        }
                    }
                }
            }
        }

        if (profile != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, RoyalBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueElevated,
                            border = BorderStroke(2.dp, NeonCyan),
                            modifier = Modifier.size(84.dp)
                        ) {
                            if (profile.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = profile.avatarUrl,
                                    contentDescription = profile.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(42.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            if (profile.verified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verificada", tint = NeonCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text("@${profile.username}", style = MaterialTheme.typography.bodyMedium, color = NeonCyan)
                        if (profile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(profile.bio, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatPill("${profile.followersCount}", "Seguidores")
                            StatPill("${profile.followingCount}", "Siguiendo")
                            StatPill("${profile.likesCount}", "Me gusta")
                            StatPill("${profile.clipsCount}", "Clips")
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        if (isOwnProfile) {
                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                border = BorderStroke(1.dp, NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar perfil y foto", color = NeonCyan)
                            }
                        } else {
                            val isFollowing = profile.userId in followingIds
                            val isBlocked = profile.userId in blockedUserIds
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onToggleFollow(profile) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) RoyalBlueElevated else NeonCyan,
                                        contentColor = if (isFollowing) TextPrimary else DeepBlack
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isFollowing) "Siguiendo" else "Seguir")
                                }
                                Button(
                                    onClick = { showGiftSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoinGold, contentColor = DeepBlack),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Regalar")
                                }
                                IconButton(
                                    onClick = { onToggleBlock(profile.userId, !isBlocked) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(RoyalBlueDark, RoundedCornerShape(12.dp))
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = "Bloquear usuario", tint = LiveCrimson)
                                }
                            }
                        }
                    }
                }
            }

            item {
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
                        text = { Text("Clips (${userClips.size})") }
                    )
                    if (isOwnProfile) {
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Favoritos (${favoriteClips.size})") }
                        )
                    }
                }
            }

            val shownClips = if (selectedTab == 0 || !isOwnProfile) userClips else favoriteClips
            if (shownClips.isEmpty()) {
                item {
                    Text(
                        text = if (selectedTab == 0) "Todavía no hay clips publicados." else "No hay favoritos guardados todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                items(shownClips, key = { it.id }) { clip ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = RoyalBlueDark,
                        border = BorderStroke(1.dp, RoyalBlueBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(clip.caption, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${clip.viewsCount} vistas • ${clip.likesCount} me gusta • ${clip.giftsCoinsTotal} monedas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            onSave = { username, display, bio, avatar ->
                onEditProfile(username, display, bio, avatar)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showGiftSheet && profile != null) {
        SendGiftBottomSheet(
            recipientUsername = profile.username,
            coinBalance = coinBalance,
            onSelectGift = { gift -> onSendGift(profile, gift) },
            onOpenWallet = onOpenWallet,
            onDismiss = { showGiftSheet = false }
        )
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onSave: (username: String, displayName: String, bio: String, avatarUrl: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(profile.username) }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl) }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUrl = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        title = { Text("Editar Perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cambiar foto de perfil", color = NeonCyan)
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario (@)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre visible") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biografía") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, displayName, bio, avatarUrl.takeIf { it.isNotBlank() }) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
