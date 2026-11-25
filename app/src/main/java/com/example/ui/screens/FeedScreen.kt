package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.core.UiState
import com.example.model.Clip
import com.example.model.ClipComment
import com.example.model.GiftCatalogItem
import com.example.model.UserProfile
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
fun FeedScreen(
    currentUserId: String,
    clipsState: UiState<List<Clip>>,
    creators: List<UserProfile>,
    followingIds: Set<String>,
    favoriteIds: Set<String>,
    blockedUserIds: Set<String>,
    recommendedOrder: List<String>,
    coinBalance: Long,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onRefreshAiRecommendations: () -> Unit,
    onRecordView: (String) -> Unit,
    onToggleLike: (Clip) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRecordShare: (String) -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    onSendGift: (Clip, GiftCatalogItem) -> Unit,
    observeComments: (String) -> Flow<List<ClipComment>>,
    onAddComment: (Clip, String) -> Unit,
    onReportClip: (Clip, String, String) -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenPublish: () -> Unit,
    onOpenWallet: () -> Unit
) {
    val context = LocalContext.current
    var feedFilter by remember { mutableStateOf("for_you") } // "for_you" | "following"
    var isFullscreenClean by remember { mutableStateOf(false) }
    var commentsClip by remember { mutableStateOf<Clip?>(null) }
    var giftClip by remember { mutableStateOf<Clip?>(null) }
    var reportClip by remember { mutableStateOf<Clip?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        when (clipsState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = clipsState.message,
                        color = LiveCrimson,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is UiState.Success -> {
                val visibleClips = remember(clipsState.data, feedFilter, followingIds, blockedUserIds, recommendedOrder) {
                    val unblocked = clipsState.data.filter { it.authorId !in blockedUserIds }
                    val filtered = if (feedFilter == "following") {
                        unblocked.filter { it.authorId in followingIds }
                    } else {
                        unblocked
                    }
                    if (recommendedOrder.isNotEmpty() && feedFilter == "for_you") {
                        val rankMap = recommendedOrder.withIndex().associate { it.value to it.index }
                        filtered.sortedBy { rankMap[it.id] ?: Int.MAX_VALUE }
                    } else {
                        filtered
                    }
                }

                if (visibleClips.isEmpty()) {
                    EmptyFeedState(
                        isFollowingTab = feedFilter == "following",
                        onOpenPublish = onOpenPublish,
                        onSwitchToForYou = { feedFilter = "for_you" }
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { visibleClips.size })
                    LaunchedEffect(pagerState.currentPage, visibleClips) {
                        val active = visibleClips.getOrNull(pagerState.currentPage)
                        if (active != null) {
                            onRecordView(active.id)
                        }
                    }

                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val clip = visibleClips[page]
                        val isActivePage = pagerState.currentPage == page
                        val creator = creators.firstOrNull { it.userId == clip.authorId }
                        val isFollowing = clip.authorId in followingIds
                        val isFavorited = clip.id in favoriteIds

                        Box(modifier = Modifier.fillMaxSize()) {
                            VideoPlayerBox(
                                videoUrl = clip.videoUrl,
                                isActive = isActivePage,
                                isMuted = isMuted,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bottom dark legibility scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, DeepBlack.copy(alpha = 0.88f))
                                        )
                                    )
                            )

                            if (!isFullscreenClean) {
                                // Right vertical interaction rail
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 12.dp, bottom = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Creator Avatar + Follow badge
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clickable { onOpenCreatorProfile(clip.authorId) }
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = RoyalBlueElevated,
                                            border = BorderStroke(2.dp, NeonCyan),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            if (clip.authorAvatarUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = clip.authorAvatarUrl,
                                                    contentDescription = clip.authorUsername,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = clip.authorUsername,
                                                        tint = NeonCyan
                                                    )
                                                }
                                            }
                                        }
                                        if (clip.authorId != currentUserId && !isFollowing && creator != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .clip(CircleShape)
                                                    .background(NeonCyan)
                                                    .clickable { onToggleFollow(creator) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Seguir",
                                                    tint = DeepBlack,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    FeedRailAction(
                                        icon = Icons.Default.Favorite,
                                        label = "${clip.likesCount}",
                                        tint = LiveCrimson,
                                        testTag = "like_button_${clip.id}",
                                        onClick = { onToggleLike(clip) }
                                    )

                                    FeedRailAction(
                                        icon = Icons.Default.ChatBubble,
                                        label = "${clip.commentsCount}",
                                        tint = TextPrimary,
                                        testTag = "comment_button_${clip.id}",
                                        onClick = { commentsClip = clip }
                                    )

                                    FeedRailAction(
                                        icon = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        label = if (isFavorited) "Guardado" else "Guardar",
                                        tint = if (isFavorited) CoinGold else TextPrimary,
                                        testTag = "favorite_button_${clip.id}",
                                        onClick = { onToggleFavorite(clip.id) }
                                    )

                                    if (clip.authorId != currentUserId) {
                                        FeedRailAction(
                                            icon = Icons.Default.CardGiftcard,
                                            label = "${clip.giftsCoinsTotal}",
                                            tint = NeonCyan,
                                            testTag = "gift_button_${clip.id}",
                                            onClick = { giftClip = clip }
                                        )
                                    }

                                    FeedRailAction(
                                        icon = Icons.Default.Share,
                                        label = "${clip.sharesCount}",
                                        tint = TextPrimary,
                                        testTag = "share_button_${clip.id}",
                                        onClick = {
                                            onRecordShare(clip.id)
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Mira este clip de @${clip.authorUsername} en Lobo Clips AI: ${clip.caption} ${clip.videoUrl}"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Compartir clip"))
                                        }
                                    )

                                    IconButton(
                                        onClick = { reportClip = clip },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = "Denunciar clip",
                                            tint = TextMuted
                                        )
                                    }
                                }

                                // Bottom left metadata overlay
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 16.dp, end = 84.dp, bottom = 22.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { onOpenCreatorProfile(clip.authorId) }
                                    ) {
                                        Text(
                                            text = "@${clip.authorUsername}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (clip.authorVerified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Cuenta verificada",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (clip.authorId != currentUserId && creator != null) {
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isFollowing) RoyalBlueDark.copy(alpha = 0.8f) else NeonCyan,
                                                modifier = Modifier.clickable { onToggleFollow(creator) }
                                            ) {
                                                Text(
                                                    text = if (isFollowing) "Siguiendo" else "Seguir",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isFollowing) TextPrimary else DeepBlack,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = clip.caption,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (clip.hashtags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = clip.hashtags.joinToString(" "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = NeonCyan,
                                            maxLines = 2
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Visualizaciones",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "${clip.viewsCount} vistas reales",
                                            style = MaterialTheme.typography.labelSmall,
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

        // Top Bar: Para ti / Siguiendo + Sound / AI Rank / Fullscreen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isFullscreenClean) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeedTabPill(
                        label = "Para ti",
                        selected = feedFilter == "for_you",
                        onClick = { feedFilter = "for_you" }
                    )
                    FeedTabPill(
                        label = "Siguiendo",
                        selected = feedFilter == "following",
                        onClick = { feedFilter = "following" }
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isFullscreenClean) {
                    IconButton(
                        onClick = onRefreshAiRecommendations,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(RoyalBlueDark.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ordenar con recomendaciones IA",
                            tint = NeonCyan
                        )
                    }
                }
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RoyalBlueDark.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Activar sonido" else "Silenciar",
                        tint = TextPrimary
                    )
                }
                IconButton(
                    onClick = { isFullscreenClean = !isFullscreenClean },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RoyalBlueDark.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isFullscreenClean) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Pantalla completa",
                        tint = TextPrimary
                    )
                }
            }
        }
    }

    commentsClip?.let { clip ->
        CommentsBottomSheet(
            clip = clip,
            commentsFlow = observeComments(clip.id),
            onAddComment = { text -> onAddComment(clip, text) },
            onDismiss = { commentsClip = null }
        )
    }

    giftClip?.let { clip ->
        SendGiftBottomSheet(
            recipientUsername = clip.authorUsername,
            coinBalance = coinBalance,
            onSelectGift = { gift -> onSendGift(clip, gift) },
            onOpenWallet = onOpenWallet,
            onDismiss = { giftClip = null }
        )
    }

    reportClip?.let { clip ->
        ReportDialog(
            title = "Denunciar clip de @${clip.authorUsername}",
            onSubmit = { reason, details ->
                onReportClip(clip, reason, details)
                reportClip = null
            },
            onDismiss = { reportClip = null }
        )
    }
}

@Composable
private fun FeedTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) NeonCyan else RoyalBlueDark.copy(alpha = 0.72f),
        border = if (selected) null else BorderStroke(1.dp, RoyalBlueBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) DeepBlack else TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun FeedRailAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ObsidianSurface.copy(alpha = 0.6f))
                .border(1.dp, RoyalBlueBorder.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}

@Composable
private fun EmptyFeedState(
    isFollowingTab: Boolean,
    onOpenPublish: () -> Unit,
    onSwitchToForYou: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_feed),
            contentDescription = "Sin vídeos todavía",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(190.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = if (isFollowingTab) "Aún no sigues a ningún creador" else "La comunidad está lista para tu primer clip",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFollowingTab) {
                "Explora la pestaña Para ti para descubrir usuarios reales y seguir su contenido."
            } else {
                "Lobo Clips AI no muestra vídeos simulados ni perfiles falsos. Graba o publica un vídeo real para iniciar el feed."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (isFollowingTab) {
            OutlinedButton(onClick = onSwitchToForYou) {
                Text("Ir a Para ti")
            }
        } else {
            Button(
                onClick = onOpenPublish,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
                modifier = Modifier.testTag("empty_feed_publish_button")
            ) {
                Text("Publicar primer clip real")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    clip: Clip,
    commentsFlow: Flow<List<ClipComment>>,
    onAddComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val comments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Comentarios reales (${comments.size})",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "Sé la primera persona real en comentar este clip.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
                items(comments, key = { it.id }) { comment ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoyalBlueDark
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "@${comment.authorUsername}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = comment.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Añade un comentario...", color = TextMuted) },
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
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText)
                            commentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(NeonCyan, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Publicar comentario",
                        tint = DeepBlack
                    )
                }
            }
        }
    }
}

@Composable
fun ReportDialog(
    title: String,
    onSubmit: (reason: String, details: String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("Contenido inapropiado") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Detalles adicionales") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason.ifBlank { "Moderación" }, details) },
                colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = TextPrimary)
            ) {
                Text("Enviar denuncia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
