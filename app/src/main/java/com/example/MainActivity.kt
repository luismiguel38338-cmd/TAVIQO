package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.UiState
import com.example.core.configureAppCheck
import com.example.data.GeminiAiRepository
import com.example.data.LoboRepository
import com.example.data.PreferencesRepository
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.ui.MainViewModel
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.GiftBurstOverlay
import com.example.ui.components.TaviqoLogoMark
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.GiftsAndWalletScreen
import com.example.ui.screens.InboxAndChatScreen
import com.example.ui.screens.LiveStreamsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PublishClipScreen
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueElevated
import com.example.ui.theme.TaviqoTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

private const val ROUTE_FEED = "feed"
private const val ROUTE_LIVE = "live"
private const val ROUTE_PUBLISH = "publish"
private const val ROUTE_INBOX = "inbox"
private const val ROUTE_WALLET = "wallet"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_CREATOR_PATTERN = "creator/{uid}"
private const val ROUTE_ADMIN = "admin"

private data class TaviqoNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val TOP_LEVEL_ITEMS = listOf(
    TaviqoNavItem(ROUTE_FEED, "Inicio", Icons.Filled.Home, Icons.Outlined.Home, "nav_feed"),
    TaviqoNavItem(ROUTE_LIVE, "Directos", Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_live"),
    TaviqoNavItem(ROUTE_PUBLISH, "Crear", Icons.Filled.AddBox, Icons.Outlined.AddBox, "nav_publish"),
    TaviqoNavItem(ROUTE_INBOX, "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_inbox"),
    TaviqoNavItem(ROUTE_WALLET, "Regalos", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_wallet"),
    TaviqoNavItem(ROUTE_PROFILE, "Perfil", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            TaviqoTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(signedInUser.uid) {
            AuthenticatedShell(
                firebaseUser = signedInUser,
                credentialManager = credentialManager
            )
        }
    }
}

@Composable
private fun AuthenticatedShell(
    firebaseUser: FirebaseUser,
    credentialManager: CredentialManager
) {
    val scope = rememberCoroutineScope()
    val viewModel: MainViewModel = viewModel(
        key = firebaseUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                MainViewModel(
                    repository = LoboRepository(app, Firebase.auth),
                    aiRepository = GeminiAiRepository(),
                    prefsRepository = PreferencesRepository(app),
                    firebaseUser = firebaseUser
                )
            }
        }
    )

    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val myWalletState by viewModel.myWalletState.collectAsStateWithLifecycle()
    val creatorsState by viewModel.creatorsState.collectAsStateWithLifecycle()
    val activeClipsState by viewModel.activeClipsState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val liveStreamsState by viewModel.liveStreamsState.collectAsStateWithLifecycle()
    val giftHistoryState by viewModel.giftHistoryState.collectAsStateWithLifecycle()
    val coinPurchasesState by viewModel.coinPurchasesState.collectAsStateWithLifecycle()
    val payoutRequestsState by viewModel.payoutRequestsState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val adminClipsState by viewModel.adminClipsState.collectAsStateWithLifecycle()
    val adminStreamsState by viewModel.adminStreamsState.collectAsStateWithLifecycle()
    val adminPurchasesState by viewModel.adminPurchasesState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val activeGiftBurst by viewModel.activeGiftBurst.collectAsStateWithLifecycle()
    val recommendedOrder by viewModel.recommendedOrder.collectAsStateWithLifecycle()
    val aiSuggestionState by viewModel.aiSuggestionState.collectAsStateWithLifecycle()
    val aiModerationState by viewModel.aiModerationState.collectAsStateWithLifecycle()
    val aiChatMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val aiChatStreaming by viewModel.aiChatStreaming.collectAsStateWithLifecycle()
    val aiChatError by viewModel.aiChatError.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    val myProfile = (myProfileState as? UiState.Success)?.data
    val myWallet = (myWalletState as? UiState.Success)?.data
    val creators = (creatorsState as? UiState.Success)?.data ?: emptyList()
    val allClips = (activeClipsState as? UiState.Success)?.data ?: emptyList()
    val blockedSet = remember(myWallet) { myWallet?.blockedUserIds?.toSet() ?: emptySet() }
    val unreadBadgeCount = remember(notificationsState, conversationsState, firebaseUser.uid) {
        val unreadNotifs = (notificationsState as? UiState.Success)?.data?.count { !it.read } ?: 0
        val unreadConvs = (conversationsState as? UiState.Success)?.data?.count {
            it.lastSenderId.isNotBlank() && it.lastSenderId != firebaseUser.uid
        } ?: 0
        unreadNotifs + unreadConvs
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTopLevel = TOP_LEVEL_ITEMS.any { it.route == currentRoute }
    var showAiAssistant by remember { mutableStateOf(false) }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = DeepBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = RoyalBlueElevated,
                        contentColor = TextPrimary,
                        actionColor = NeonCyan,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            bottomBar = {
                if (!useRail && isTopLevel) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary
                    ) {
                        TOP_LEVEL_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    if (item.route == ROUTE_INBOX && unreadBadgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = LiveCrimson, contentColor = TextPrimary) {
                                                    Text(if (unreadBadgeCount > 9) "9+" else "$unreadBadgeCount")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = RoyalBlueElevated
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail && isTopLevel) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            contentColor = TextPrimary,
                            header = {
                                TaviqoLogoMark(
                                    size = 34.dp,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                )
                                IconButton(
                                    onClick = { showAiAssistant = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("rail_ai_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Asistente Gemini AI",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            TOP_LEVEL_ITEMS.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        if (item.route == ROUTE_INBOX && unreadBadgeCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = LiveCrimson, contentColor = TextPrimary) {
                                                        Text(if (unreadBadgeCount > 9) "9+" else "$unreadBadgeCount")
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.label
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    },
                                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = RoyalBlueElevated
                                    ),
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_FEED,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ROUTE_FEED) {
                                FeedScreen(
                                    currentUserId = firebaseUser.uid,
                                    clipsState = activeClipsState,
                                    creators = creators,
                                    followingIds = followingIds,
                                    favoriteIds = favoriteIds,
                                    blockedUserIds = blockedSet,
                                    recommendedOrder = recommendedOrder,
                                    coinBalance = myWallet?.coinBalance ?: 0L,
                                    isMuted = isMuted,
                                    onToggleMute = viewModel::toggleMuted,
                                    onRefreshAiRecommendations = viewModel::refreshRecommendationsFromRealActivity,
                                    onRecordView = viewModel::recordViewIfNeeded,
                                    onToggleLike = viewModel::toggleLike,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onRecordShare = viewModel::recordShare,
                                    onToggleFollow = viewModel::toggleFollow,
                                    onSendGift = { clip, gift ->
                                        viewModel.sendVirtualGift(
                                            recipientId = clip.authorId,
                                            recipientUsername = clip.authorUsername,
                                            gift = gift,
                                            contextType = "clip",
                                            contextId = clip.id
                                        )
                                    },
                                    observeComments = viewModel::observeClipComments,
                                    onAddComment = viewModel::addComment,
                                    onReportClip = { clip, reason, details ->
                                        viewModel.submitReport("clip", clip.id, reason, details)
                                    },
                                    onOpenCreatorProfile = { uid ->
                                        if (uid == firebaseUser.uid) {
                                            navigateTopLevel(ROUTE_PROFILE)
                                        } else {
                                            navController.navigate("creator/$uid")
                                        }
                                    },
                                    onOpenPublish = { navigateTopLevel(ROUTE_PUBLISH) },
                                    onOpenWallet = { navigateTopLevel(ROUTE_WALLET) }
                                )
                            }

                            composable(ROUTE_LIVE) {
                                LiveStreamsScreen(
                                    currentUserId = firebaseUser.uid,
                                    streamsState = liveStreamsState,
                                    blockedUserIds = blockedSet,
                                    coinBalance = myWallet?.coinBalance ?: 0L,
                                    observeChat = viewModel::observeLiveChat,
                                    onStartLiveStream = viewModel::startLiveStream,
                                    onEndLiveStream = viewModel::endLiveStream,
                                    onJoinStream = viewModel::joinLiveStream,
                                    onLeaveStream = viewModel::leaveLiveStream,
                                    onSendChat = viewModel::sendLiveChat,
                                    onSendGift = { stream, gift ->
                                        viewModel.sendVirtualGift(
                                            recipientId = stream.hostId,
                                            recipientUsername = stream.hostUsername,
                                            gift = gift,
                                            contextType = "live",
                                            contextId = stream.id
                                        )
                                    },
                                    onBlockHost = { hostId -> viewModel.toggleBlockUser(hostId, true) },
                                    onReportStream = { stream, reason, details ->
                                        viewModel.submitReport("live", stream.id, reason, details)
                                    },
                                    onOpenWallet = { navigateTopLevel(ROUTE_WALLET) }
                                )
                            }

                            composable(ROUTE_PUBLISH) {
                                PublishClipScreen(
                                    isPublishing = isPublishing,
                                    aiSuggestionState = aiSuggestionState,
                                    aiModerationState = aiModerationState,
                                    onRequestAiSuggestions = viewModel::requestAiSuggestions,
                                    onClearAiSuggestions = viewModel::clearAiSuggestions,
                                    onPublishClip = { caption, hashtags, videoUrl, durationMs ->
                                        viewModel.publishClip(
                                            caption = caption,
                                            hashtagsRaw = hashtags,
                                            videoUrl = videoUrl,
                                            durationMs = durationMs,
                                            onPublished = { navigateTopLevel(ROUTE_FEED) }
                                        )
                                    }
                                )
                            }

                            composable(ROUTE_INBOX) {
                                InboxAndChatScreen(
                                    currentUserId = firebaseUser.uid,
                                    creators = creators,
                                    blockedUserIds = blockedSet,
                                    conversationsState = conversationsState,
                                    notificationsState = notificationsState,
                                    observeMessages = viewModel::observeDirectMessages,
                                    onOpenOrCreateConversation = viewModel::openOrCreateConversation,
                                    onSendDirectMessage = viewModel::sendDirectMessage,
                                    onMarkConversationRead = viewModel::markConversationRead,
                                    onMarkNotificationRead = viewModel::markNotificationRead,
                                    onToggleBlock = viewModel::toggleBlockUser
                                )
                            }

                            composable(ROUTE_WALLET) {
                                GiftsAndWalletScreen(
                                    currentUserId = firebaseUser.uid,
                                    walletState = myWalletState,
                                    giftHistoryState = giftHistoryState,
                                    purchasesState = coinPurchasesState,
                                    payoutsState = payoutRequestsState,
                                    onCreateCoinOrder = viewModel::createCoinPurchaseOrder,
                                    onRequestPayout = viewModel::requestPayout
                                )
                            }

                            composable(ROUTE_PROFILE) {
                                ProfileScreen(
                                    currentUserId = firebaseUser.uid,
                                    viewedProfile = myProfile,
                                    allClips = allClips,
                                    favoriteIds = favoriteIds,
                                    followingIds = followingIds,
                                    blockedUserIds = blockedSet,
                                    coinBalance = myWallet?.coinBalance ?: 0L,
                                    isAdmin = isAdmin,
                                    isOwnProfile = true,
                                    onBack = null,
                                    onEditProfile = viewModel::updateProfile,
                                    onToggleFollow = viewModel::toggleFollow,
                                    onToggleBlock = viewModel::toggleBlockUser,
                                    onSendGift = { target, gift ->
                                        viewModel.sendVirtualGift(
                                            recipientId = target.userId,
                                            recipientUsername = target.username,
                                            gift = gift,
                                            contextType = "profile",
                                            contextId = target.userId
                                        )
                                    },
                                    onOpenWallet = { navigateTopLevel(ROUTE_WALLET) },
                                    onOpenAiAssistant = { showAiAssistant = true },
                                    onOpenAdminPanel = {
                                        if (isAdmin) navController.navigate(ROUTE_ADMIN)
                                    },
                                    onSignOut = {
                                        signOut(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    }
                                )
                            }

                            composable(
                                route = ROUTE_CREATOR_PATTERN,
                                arguments = listOf(navArgument("uid") { type = NavType.StringType })
                            ) { backStack ->
                                val targetUid = backStack.arguments?.getString("uid").orEmpty()
                                val targetProfile = creators.firstOrNull { it.userId == targetUid }
                                ProfileScreen(
                                    currentUserId = firebaseUser.uid,
                                    viewedProfile = targetProfile,
                                    allClips = allClips,
                                    favoriteIds = favoriteIds,
                                    followingIds = followingIds,
                                    blockedUserIds = blockedSet,
                                    coinBalance = myWallet?.coinBalance ?: 0L,
                                    isAdmin = isAdmin,
                                    isOwnProfile = targetUid == firebaseUser.uid,
                                    onBack = { navController.popBackStack() },
                                    onEditProfile = viewModel::updateProfile,
                                    onToggleFollow = viewModel::toggleFollow,
                                    onToggleBlock = viewModel::toggleBlockUser,
                                    onSendGift = { target, gift ->
                                        viewModel.sendVirtualGift(
                                            recipientId = target.userId,
                                            recipientUsername = target.username,
                                            gift = gift,
                                            contextType = "profile",
                                            contextId = target.userId
                                        )
                                    },
                                    onOpenWallet = { navigateTopLevel(ROUTE_WALLET) },
                                    onOpenAiAssistant = { showAiAssistant = true },
                                    onOpenAdminPanel = {
                                        if (isAdmin) navController.navigate(ROUTE_ADMIN)
                                    },
                                    onSignOut = {}
                                )
                            }

                            composable(ROUTE_ADMIN) {
                                if (isAdmin) {
                                    AdminPanelScreen(
                                        creators = creators,
                                        reportsState = adminReportsState,
                                        clipsState = adminClipsState,
                                        streamsState = adminStreamsState,
                                        purchasesState = adminPurchasesState,
                                        onModerateClip = viewModel::adminModerateClip,
                                        onTerminateStream = viewModel::adminTerminateStream,
                                        onUpdateUserFlags = viewModel::adminUpdateUserFlags,
                                        onResolveReport = viewModel::adminResolveReport,
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(modifier = Modifier.fillMaxSize().background(DeepBlack))
                                }
                            }
                        }

                        GiftBurstOverlay(
                            burst = activeGiftBurst,
                            onDismiss = viewModel::dismissGiftBurst,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 64.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAiAssistant) {
        AiAssistantSheet(
            messages = aiChatMessages,
            streamingText = aiChatStreaming,
            errorMessage = aiChatError,
            onSendPrompt = viewModel::sendAiAssistantPrompt,
            onDismiss = { showAiAssistant = false }
        )
    }
}
