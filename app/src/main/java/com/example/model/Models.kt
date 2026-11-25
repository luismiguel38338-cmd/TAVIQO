package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val verified: Boolean = false,
    val suspended: Boolean = false,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val clipsCount: Long = 0L,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = "user",
    val coinBalance: Long = 0L,
    val creatorEarningsCoins: Long = 0L,
    val totalSpentCoins: Long = 0L,
    val blockedUserIds: List<String> = emptyList(),
    val lastTxId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowEdge(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val targetUserId: String = "",
    val targetUsername: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteClip(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clipId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val type: String = "system",
    val title: String = "",
    val body: String = "",
    val referenceId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Clip(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val authorVerified: Boolean = false,
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val durationMs: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val giftsCoinsTotal: Long = 0L,
    val status: String = "active",
    val aiModerationStatus: String = "approved",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClipComment(
    @DocumentId val id: String = "",
    val clipId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveStream(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostUsername: String = "",
    val hostAvatarUrl: String = "",
    val title: String = "",
    val category: String = "General",
    val streamUrl: String = "",
    val status: String = "live",
    val viewerCount: Long = 0L,
    val giftsCoinsTotal: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveChatMessage(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val kind: String = "chat",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GiftTransaction(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val recipientId: String = "",
    val recipientUsername: String = "",
    val giftId: String = "",
    val giftName: String = "",
    val coinCost: Long = 0L,
    val creatorNetCoins: Long = 0L,
    val platformFeeCoins: Long = 0L,
    val contextType: String = "clip",
    val contextId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CoinPurchase(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageId: String = "",
    val coinsAmount: Long = 0L,
    val priceUsdCents: Long = 0L,
    val provider: String = "google_play_billing",
    val status: String = "pending_verification",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutRequest(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val coinsAmount: Long = 0L,
    val estimatedUsdCents: Long = 0L,
    val provider: String = "stripe_connect",
    val destinationHint: String = "",
    val status: String = "pending_review",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val unreadUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val readBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "clip",
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GiftCatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val coinCost: Long,
    val animationTier: String,
    val badgeSymbol: String,
    val isExclusive: Boolean = false
) {
    val creatorNetCoins: Long get() = (coinCost * 70L) / 100L
    val platformFeeCoins: Long get() = coinCost - creatorNetCoins
}

data class CoinPackage(
    val id: String,
    val title: String,
    val coinsAmount: Long,
    val priceUsdCents: Long,
    val bonusLabel: String
)

data class GiftBurstEvent(
    val id: String,
    val gift: GiftCatalogItem,
    val senderUsername: String,
    val recipientUsername: String
)

data class AiModerationResult(
    val safe: Boolean,
    val riskLevel: String,
    val reason: String,
    val suggestedAction: String
)

data class AiCreatorSuggestion(
    val caption: String,
    val hashtags: List<String>
)

object TaviqoCatalog {
    val gifts: List<GiftCatalogItem> = listOf(
        GiftCatalogItem(
            id = "rose",
            name = "Rosa",
            description = "Detalle clásico para apoyar a tu creador",
            coinCost = 5L,
            animationTier = "sparkle",
            badgeSymbol = "Rosa"
        ),
        GiftCatalogItem(
            id = "heart",
            name = "Corazón",
            description = "Pulso de energía cyan eléctrico",
            coinCost = 15L,
            animationTier = "pulse",
            badgeSymbol = "Corazón"
        ),
        GiftCatalogItem(
            id = "diamond",
            name = "Diamante",
            description = "Destello cristalino azul royal",
            coinCost = 50L,
            animationTier = "prism",
            badgeSymbol = "Diamante"
        ),
        GiftCatalogItem(
            id = "crown",
            name = "Corona Imperial",
            description = "Brillo dorado sobre todo el escenario",
            coinCost = 150L,
            animationTier = "spotlight",
            badgeSymbol = "Corona"
        ),
        GiftCatalogItem(
            id = "rocket",
            name = "Cohete",
            description = "Impulso supersónico hacia tendencias",
            coinCost = 300L,
            animationTier = "launch",
            badgeSymbol = "Cohete"
        ),
        GiftCatalogItem(
            id = "taviqo_nova",
            name = "Supernova Taviqo",
            description = "Fulgor prismático exclusivo de TAVIQO",
            coinCost = 1000L,
            animationTier = "legendary",
            badgeSymbol = "Supernova",
            isExclusive = true
        )
    )

    val coinPackages: List<CoinPackage> = listOf(
        CoinPackage("pack_100", "Pack Inicio", 100L, 199L, "Estándar"),
        CoinPackage("pack_500", "Pack Creador", 500L, 799L, "+5% valor"),
        CoinPackage("pack_1200", "Pack Royal", 1200L, 1699L, "+15% valor"),
        CoinPackage("pack_3000", "Pack Escenario VIP", 3000L, 3999L, "Exclusivo")
    )
}
