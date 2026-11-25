package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.core.unwrapFirestoreError
import com.example.model.Clip
import com.example.model.ClipComment
import com.example.model.CoinPackage
import com.example.model.CoinPurchase
import com.example.model.Conversation
import com.example.model.DirectMessage
import com.example.model.FavoriteClip
import com.example.model.FollowEdge
import com.example.model.GiftCatalogItem
import com.example.model.GiftTransaction
import com.example.model.LiveChatMessage
import com.example.model.LiveStream
import com.example.model.ModerationReport
import com.example.model.PayoutRequest
import com.example.model.PrivateAccount
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class LoboRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    constructor(context: Context, auth: FirebaseAuth) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = auth
    )

    private val estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Debes iniciar sesión con Google para continuar.")
    }

    private fun isSafeCurrentUser(uid: String): Boolean {
        return auth.currentUser?.uid == uid
    }

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, uid: String): Flow<T> {
        return catch { error ->
            val root = unwrapFirestoreError(error)
            if (isSafeCurrentUser(uid) && root is Exception) {
                handleFirestoreError(root, op, path)
            }
            throw error
        }
    }

    suspend fun ensureUserBootstrap(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        return try {
            val userRef = db.collection("users").document(uid)
            val walletRef = userRef.collection("private").document("wallet")

            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val baseName = (user.displayName ?: user.email?.substringBefore("@") ?: "taviqo")
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .trim('_')
                    .ifBlank { "taviqo" }
                val handle = (baseName.take(20) + "_" + uid.take(4)).take(30).padEnd(3, '0')
                val display = (user.displayName ?: "Creador Taviqo").trim().take(40).ifBlank { "Creador Taviqo" }
                val profileMap = buildMap<String, Any> {
                    put("userId", uid)
                    put("username", handle)
                    put("displayName", display)
                    put("bio", "Creador en TAVIQO • El mundo es tu escenario.")
                    user.photoUrl?.toString()?.take(500)?.let { put("avatarUrl", it) }
                    put("verified", false)
                    put("suspended", false)
                    put("followersCount", 0L)
                    put("followingCount", 0L)
                    put("clipsCount", 0L)
                    put("likesCount", 0L)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userRef.set(profileMap).await()
            }

            val walletSnap = walletRef.get().await()
            if (!walletSnap.exists()) {
                val walletMap = buildMap<String, Any> {
                    put("userId", uid)
                    user.email?.take(160)?.let { put("email", it) }
                    put("role", "user")
                    put("coinBalance", 0L)
                    put("creatorEarningsCoins", 0L)
                    put("totalSpentCoins", 0L)
                    put("blockedUserIds", emptyList<String>())
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                walletRef.set(walletMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observeMyProfile(uid: String): Flow<UserProfile?> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("users").document(uid).snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, estimate) }
                .logFirestoreErrors(OperationType.GET, "users/$uid", uid)
        )
    }

    fun observeMyWallet(uid: String): Flow<PrivateAccount?> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("users").document(uid).collection("private").document("wallet").snapshots()
                .map { snap -> snap.toObject(PrivateAccount::class.java, estimate) }
                .logFirestoreErrors(OperationType.GET, "users/$uid/private/wallet", uid)
        )
    }

    fun observeCreators(uid: String): Flow<List<UserProfile>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("users")
                .whereEqualTo("suspended", false)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, estimate)
                        .sortedByDescending { it.followersCount }
                }
                .logFirestoreErrors(OperationType.LIST, "users", uid)
        )
    }

    fun observeActiveClips(uid: String): Flow<List<Clip>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("clips")
                .whereEqualTo("status", "active")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Clip::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "clips", uid)
        )
    }

    fun observeFollowingIds(uid: String): Flow<Set<String>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("users").document(uid).collection("following")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .logFirestoreErrors(OperationType.LIST, "users/$uid/following", uid)
        )
    }

    fun observeFavoriteIds(uid: String): Flow<Set<String>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("users").document(uid).collection("favorites")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .logFirestoreErrors(OperationType.LIST, "users/$uid/favorites", uid)
        )
    }

    fun observeNotifications(uid: String): Flow<List<UserNotification>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "users/$uid/notifications", uid)
        )
    }

    fun observeClipComments(clipId: String, uid: String): Flow<List<ClipComment>> = flow {
        require(clipId.isNotBlank() && uid.isNotBlank())
        emitAll(
            db.collection("clips").document(clipId).collection("comments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ClipComment::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "clips/$clipId/comments", uid)
        )
    }

    fun observeLiveStreams(uid: String): Flow<List<LiveStream>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("live_streams")
                .whereEqualTo("status", "live")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveStream::class.java, estimate)
                        .sortedByDescending { it.viewerCount }
                }
                .logFirestoreErrors(OperationType.LIST, "live_streams", uid)
        )
    }

    fun observeLiveChat(streamId: String, uid: String): Flow<List<LiveChatMessage>> = flow {
        require(streamId.isNotBlank() && uid.isNotBlank())
        emitAll(
            db.collection("live_streams").document(streamId).collection("chat")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveChatMessage::class.java, estimate)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "live_streams/$streamId/chat", uid)
        )
    }

    fun observeGiftHistory(uid: String): Flow<List<GiftTransaction>> {
        val sentFlow = db.collection("gift_transactions")
            .whereEqualTo("senderId", uid)
            .snapshots()
            .map { it.toObjects(GiftTransaction::class.java, estimate) }
        val receivedFlow = db.collection("gift_transactions")
            .whereEqualTo("recipientId", uid)
            .snapshots()
            .map { it.toObjects(GiftTransaction::class.java, estimate) }
        return combine(sentFlow, receivedFlow) { sent, received ->
            (sent + received)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }.logFirestoreErrors(OperationType.LIST, "gift_transactions", uid)
    }

    fun observeCoinPurchases(uid: String): Flow<List<CoinPurchase>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("coin_purchases")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CoinPurchase::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "coin_purchases", uid)
        )
    }

    fun observePayoutRequests(uid: String): Flow<List<PayoutRequest>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("payout_requests")
                .whereEqualTo("creatorId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PayoutRequest::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "payout_requests", uid)
        )
    }

    fun observeConversations(uid: String): Flow<List<Conversation>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, estimate)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "conversations", uid)
        )
    }

    fun observeDirectMessages(conversationId: String, uid: String): Flow<List<DirectMessage>> = flow {
        require(conversationId.isNotBlank() && uid.isNotBlank())
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectMessage::class.java, estimate)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "conversations/$conversationId/messages", uid)
        )
    }

    suspend fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val activeUid = requireUserId()
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").take(30)
            require(cleanUsername.length >= 3) { "El nombre de usuario debe tener al menos 3 caracteres." }
            val cleanDisplay = displayName.trim().take(40)
            require(cleanDisplay.isNotEmpty()) { "El nombre visible no puede estar vacío." }

            val updates = buildMap<String, Any> {
                put("username", cleanUsername)
                put("displayName", cleanDisplay)
                put("bio", bio.trim().take(200))
                if (!avatarUrl.isNullOrBlank()) {
                    put("avatarUrl", avatarUrl.trim().take(500))
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(activeUid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun publishClip(
        myProfile: UserProfile,
        caption: String,
        hashtags: List<String>,
        videoUrl: String,
        durationMs: Long,
        aiModerationStatus: String
    ): Result<String> {
        var path = "clips"
        return try {
            val uid = requireUserId()
            require(videoUrl.isNotBlank()) { "Selecciona o graba un vídeo válido primero." }
            val cleanCaption = caption.trim().take(300)
            val cleanTags = hashtags
                .map { it.trim().take(32) }
                .filter { it.isNotBlank() }
                .take(10)

            val clipRef = db.collection("clips").document()
            path = clipRef.path
            val payload = buildMap<String, Any> {
                put("authorId", uid)
                put("authorUsername", myProfile.username.ifBlank { "taviqo_user" }.take(30))
                myProfile.avatarUrl.takeIf { it.isNotBlank() }?.let { put("authorAvatarUrl", it.take(500)) }
                put("authorVerified", myProfile.verified)
                put("caption", cleanCaption)
                put("hashtags", cleanTags)
                put("videoUrl", videoUrl.trim().take(1000))
                put("durationMs", durationMs.coerceIn(0L, 600_000L))
                put("likesCount", 0L)
                put("commentsCount", 0L)
                put("viewsCount", 0L)
                put("sharesCount", 0L)
                put("giftsCoinsTotal", 0L)
                put("status", "active")
                put("aiModerationStatus", aiModerationStatus)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            val batch = db.batch()
            batch.set(clipRef, payload)
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "clipsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(clipRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeClip(clip: Clip): Result<Boolean> {
        val path = "clips/${clip.id}"
        return try {
            val uid = requireUserId()
            val clipRef = db.collection("clips").document(clip.id)
            val likeRef = clipRef.collection("likes").document(uid)
            val authorRef = db.collection("users").document(clip.authorId)

            val likedNow = db.runTransaction { tx ->
                val likeSnap = tx.get(likeRef)
                val clipSnap = tx.get(clipRef)
                val currentLikes = clipSnap.getLong("likesCount") ?: 0L
                val authorSnap = tx.get(authorRef)
                val currentAuthorLikes = authorSnap.getLong("likesCount") ?: 0L

                if (likeSnap.exists()) {
                    tx.delete(likeRef)
                    tx.update(
                        clipRef,
                        mapOf(
                            "likesCount" to max(0L, currentLikes - 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (authorSnap.exists()) {
                        tx.update(
                            authorRef,
                            mapOf(
                                "likesCount" to max(0L, currentAuthorLikes - 1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    false
                } else {
                    tx.set(
                        likeRef,
                        mapOf(
                            "userId" to uid,
                            "clipId" to clip.id,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        clipRef,
                        mapOf(
                            "likesCount" to currentLikes + 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (authorSnap.exists()) {
                        tx.update(
                            authorRef,
                            mapOf(
                                "likesCount" to currentAuthorLikes + 1L,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    true
                }
            }.await()
            Result.success(likedNow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteClip(clipId: String): Result<Boolean> {
        var path = "users/favorites/$clipId"
        return try {
            val uid = requireUserId()
            val favRef = db.collection("users").document(uid).collection("favorites").document(clipId)
            path = favRef.path
            val snap = favRef.get().await()
            if (snap.exists()) {
                favRef.delete().await()
                Result.success(false)
            } else {
                favRef.set(
                    mapOf(
                        "userId" to uid,
                        "clipId" to clipId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementClipView(clipId: String): Result<Unit> {
        val path = "clips/$clipId"
        return try {
            requireUserId()
            db.collection("clips").document(clipId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementClipShare(clipId: String): Result<Unit> {
        val path = "clips/$clipId"
        return try {
            requireUserId()
            db.collection("clips").document(clipId).update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(myProfile: UserProfile, clip: Clip, text: String): Result<Unit> {
        val cleanText = text.trim().take(500)
        var path = "clips/${clip.id}/comments"
        return try {
            val uid = requireUserId()
            require(cleanText.isNotEmpty()) { "Escribe un comentario." }
            val clipRef = db.collection("clips").document(clip.id)
            val commentRef = clipRef.collection("comments").document()
            path = commentRef.path

            val payload = buildMap<String, Any> {
                put("clipId", clip.id)
                put("authorId", uid)
                put("authorUsername", myProfile.username.ifBlank { "taviqo_user" }.take(30))
                myProfile.avatarUrl.takeIf { it.isNotBlank() }?.let { put("authorAvatarUrl", it.take(500)) }
                put("text", cleanText)
                put("createdAt", FieldValue.serverTimestamp())
            }

            val batch = db.batch()
            batch.set(commentRef, payload)
            batch.update(
                clipRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            if (clip.authorId != uid) {
                sendNotification(
                    recipientId = clip.authorId,
                    actorId = uid,
                    actorUsername = myProfile.username,
                    type = "comment",
                    title = "Nuevo comentario",
                    body = "@${myProfile.username} comentó: ${cleanText.take(80)}",
                    referenceId = clip.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(myProfile: UserProfile, targetUser: UserProfile): Result<Boolean> {
        val targetUid = targetUser.userId
        return try {
            val uid = requireUserId()
            require(uid != targetUid) { "No puedes seguirte a ti mismo." }
            val myRef = db.collection("users").document(uid)
            val targetRef = db.collection("users").document(targetUid)
            val followRef = myRef.collection("following").document(targetUid)

            val followingNow = db.runTransaction { tx ->
                val followSnap = tx.get(followRef)
                val mySnap = tx.get(myRef)
                val targetSnap = tx.get(targetRef)
                val myFollowing = mySnap.getLong("followingCount") ?: 0L
                val targetFollowers = targetSnap.getLong("followersCount") ?: 0L

                if (followSnap.exists()) {
                    tx.delete(followRef)
                    tx.update(myRef, mapOf("followingCount" to max(0L, myFollowing - 1L), "updatedAt" to FieldValue.serverTimestamp()))
                    tx.update(targetRef, mapOf("followersCount" to max(0L, targetFollowers - 1L), "updatedAt" to FieldValue.serverTimestamp()))
                    false
                } else {
                    tx.set(
                        followRef,
                        mapOf(
                            "followerId" to uid,
                            "targetUserId" to targetUid,
                            "targetUsername" to targetUser.username.ifBlank { "creador" }.take(30),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(myRef, mapOf("followingCount" to myFollowing + 1L, "updatedAt" to FieldValue.serverTimestamp()))
                    tx.update(targetRef, mapOf("followersCount" to targetFollowers + 1L, "updatedAt" to FieldValue.serverTimestamp()))
                    true
                }
            }.await()

            if (followingNow) {
                sendNotification(
                    recipientId = targetUid,
                    actorId = uid,
                    actorUsername = myProfile.username,
                    type = "follow",
                    title = "Nuevo seguidor",
                    body = "@${myProfile.username} empezó a seguirte en TAVIQO.",
                    referenceId = uid
                )
            }
            Result.success(followingNow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$targetUid/following")
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(targetUid: String, block: Boolean): Result<Unit> {
        var path = "users/private/wallet"
        return try {
            val uid = requireUserId()
            require(uid != targetUid)
            val walletRef = db.collection("users").document(uid).collection("private").document("wallet")
            path = walletRef.path
            val updateValue = if (block) FieldValue.arrayUnion(targetUid) else FieldValue.arrayRemove(targetUid)
            walletRef.update(
                mapOf(
                    "blockedUserIds" to updateValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun startLiveStream(
        myProfile: UserProfile,
        title: String,
        category: String,
        streamUrl: String
    ): Result<String> {
        var path = "live_streams"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 2) { "El título del directo debe tener al menos 2 caracteres." }
            val cleanUrl = streamUrl.trim().take(1000)
            require(cleanUrl.isNotEmpty()) { "Ingresa la URL real del stream HLS/RTMP o de cámara." }

            val streamRef = db.collection("live_streams").document()
            path = streamRef.path
            val payload = buildMap<String, Any> {
                put("hostId", uid)
                put("hostUsername", myProfile.username.ifBlank { "host_taviqo" }.take(30))
                myProfile.avatarUrl.takeIf { it.isNotBlank() }?.let { put("hostAvatarUrl", it.take(500)) }
                put("title", cleanTitle)
                put("category", category.trim().ifBlank { "General" }.take(40))
                put("streamUrl", cleanUrl)
                put("status", "live")
                put("viewerCount", 0L)
                put("giftsCoinsTotal", 0L)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            streamRef.set(payload).await()
            Result.success(streamRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun endLiveStream(streamId: String): Result<Unit> {
        val path = "live_streams/$streamId"
        return try {
            requireUserId()
            db.collection("live_streams").document(streamId).update(
                mapOf(
                    "status" to "ended",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinLiveStream(streamId: String): Result<Unit> {
        val path = "live_streams/$streamId/viewers"
        return try {
            val uid = requireUserId()
            val streamRef = db.collection("live_streams").document(streamId)
            val viewerRef = streamRef.collection("viewers").document(uid)

            db.runTransaction { tx ->
                val viewerSnap = tx.get(viewerRef)
                val streamSnap = tx.get(streamRef)
                val count = streamSnap.getLong("viewerCount") ?: 0L
                if (!viewerSnap.exists()) {
                    tx.set(
                        viewerRef,
                        mapOf(
                            "userId" to uid,
                            "streamId" to streamId,
                            "joinedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        streamRef,
                        mapOf(
                            "viewerCount" to count + 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveLiveStream(streamId: String): Result<Unit> {
        val path = "live_streams/$streamId/viewers"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val streamRef = db.collection("live_streams").document(streamId)
            val viewerRef = streamRef.collection("viewers").document(uid)

            db.runTransaction { tx ->
                val viewerSnap = tx.get(viewerRef)
                val streamSnap = tx.get(streamRef)
                val count = streamSnap.getLong("viewerCount") ?: 0L
                if (viewerSnap.exists()) {
                    tx.delete(viewerRef)
                    tx.update(
                        streamRef,
                        mapOf(
                            "viewerCount" to max(0L, count - 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendLiveChat(
        myProfile: UserProfile,
        streamId: String,
        text: String,
        kind: String = "chat"
    ): Result<Unit> {
        val path = "live_streams/$streamId/chat"
        return try {
            val uid = requireUserId()
            val clean = text.trim().take(300)
            require(clean.isNotEmpty()) { "El mensaje no puede estar vacío." }
            db.collection("live_streams").document(streamId).collection("chat").document()
                .set(
                    mapOf(
                        "streamId" to streamId,
                        "senderId" to uid,
                        "senderUsername" to myProfile.username.ifBlank { "taviqo_user" }.take(30),
                        "text" to clean,
                        "kind" to kind,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendVirtualGift(
        senderProfile: UserProfile,
        recipientId: String,
        recipientUsername: String,
        gift: GiftCatalogItem,
        contextType: String,
        contextId: String
    ): Result<GiftTransaction> {
        var path = "gift_transactions"
        return try {
            val uid = requireUserId()
            require(uid != recipientId) { "No puedes enviarte regalos a ti mismo." }

            val txRef = db.collection("gift_transactions").document()
            val txId = txRef.id
            path = txRef.path

            val senderWalletRef = db.collection("users").document(uid).collection("private").document("wallet")
            val recipientWalletRef = db.collection("users").document(recipientId).collection("private").document("wallet")
            val contextRef = when (contextType) {
                "clip" -> db.collection("clips").document(contextId)
                "live" -> db.collection("live_streams").document(contextId)
                else -> null
            }

            db.runTransaction { tx ->
                val senderSnap = tx.get(senderWalletRef)
                val recipientSnap = tx.get(recipientWalletRef)
                if (!senderSnap.exists()) throw IllegalStateException("Billetera no inicializada.")
                if (!recipientSnap.exists()) throw IllegalStateException("La billetera del creador no está disponible.")

                val currentBalance = senderSnap.getLong("coinBalance") ?: 0L
                val currentSpent = senderSnap.getLong("totalSpentCoins") ?: 0L
                if (currentBalance < gift.coinCost) {
                    throw IllegalStateException("Saldo insuficiente (${currentBalance} monedas). Adquiere monedas primero.")
                }
                val recipientEarnings = recipientSnap.getLong("creatorEarningsCoins") ?: 0L

                val txPayload = mapOf(
                    "senderId" to uid,
                    "senderUsername" to senderProfile.username.ifBlank { "usuario" }.take(30),
                    "recipientId" to recipientId,
                    "recipientUsername" to recipientUsername.ifBlank { "creador" }.take(30),
                    "giftId" to gift.id,
                    "giftName" to gift.name,
                    "coinCost" to gift.coinCost,
                    "creatorNetCoins" to gift.creatorNetCoins,
                    "platformFeeCoins" to gift.platformFeeCoins,
                    "contextType" to contextType,
                    "contextId" to contextId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                tx.set(txRef, txPayload)

                tx.update(
                    senderWalletRef,
                    mapOf(
                        "coinBalance" to (currentBalance - gift.coinCost),
                        "totalSpentCoins" to (currentSpent + gift.coinCost),
                        "lastTxId" to txId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.update(
                    recipientWalletRef,
                    mapOf(
                        "creatorEarningsCoins" to (recipientEarnings + gift.creatorNetCoins),
                        "lastTxId" to txId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (contextRef != null) {
                    val ctxSnap = tx.get(contextRef)
                    val prevGiftCoins = ctxSnap.getLong("giftsCoinsTotal") ?: 0L
                    tx.update(
                        contextRef,
                        mapOf(
                            "giftsCoinsTotal" to (prevGiftCoins + gift.coinCost),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            sendNotification(
                recipientId = recipientId,
                actorId = uid,
                actorUsername = senderProfile.username,
                type = "gift",
                title = "Recibiste un ${gift.name}",
                body = "@${senderProfile.username} te envió ${gift.name} (+${gift.creatorNetCoins} monedas netas).",
                referenceId = txId
            )

            if (contextType == "live") {
                sendLiveChat(
                    myProfile = senderProfile,
                    streamId = contextId,
                    text = "Envió ${gift.name} (${gift.coinCost} monedas)",
                    kind = "gift"
                )
            }

            Result.success(
                GiftTransaction(
                    id = txId,
                    senderId = uid,
                    senderUsername = senderProfile.username,
                    recipientId = recipientId,
                    recipientUsername = recipientUsername,
                    giftId = gift.id,
                    giftName = gift.name,
                    coinCost = gift.coinCost,
                    creatorNetCoins = gift.creatorNetCoins,
                    platformFeeCoins = gift.platformFeeCoins,
                    contextType = contextType,
                    contextId = contextId
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createCoinPurchaseOrder(pkg: CoinPackage, provider: String = "google_play_billing"): Result<String> {
        var path = "coin_purchases"
        return try {
            val uid = requireUserId()
            val ref = db.collection("coin_purchases").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "packageId" to pkg.id,
                    "coinsAmount" to pkg.coinsAmount,
                    "priceUsdCents" to pkg.priceUsdCents,
                    "provider" to provider,
                    "status" to "pending_verification",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun requestCreatorPayout(
        coinsAmount: Long,
        provider: String,
        destinationHint: String
    ): Result<String> {
        var path = "payout_requests"
        return try {
            val uid = requireUserId()
            require(coinsAmount >= 100L) { "El mínimo de retiro es de 100 monedas de creador." }
            val cleanHint = destinationHint.trim().take(120)
            require(cleanHint.length >= 3) { "Indica un destino autorizado válido." }

            val reqRef = db.collection("payout_requests").document()
            val reqId = reqRef.id
            path = reqRef.path
            val walletRef = db.collection("users").document(uid).collection("private").document("wallet")

            db.runTransaction { tx ->
                val walletSnap = tx.get(walletRef)
                val earnings = walletSnap.getLong("creatorEarningsCoins") ?: 0L
                if (earnings < coinsAmount) {
                    throw IllegalStateException("Ganancias insuficientes ($earnings monedas disponibles).")
                }
                tx.set(
                    reqRef,
                    mapOf(
                        "creatorId" to uid,
                        "coinsAmount" to coinsAmount,
                        "estimatedUsdCents" to coinsAmount, // 1 creator coin = $0.01 USD estimate
                        "provider" to provider.take(64),
                        "destinationHint" to cleanHint,
                        "status" to "pending_review",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    walletRef,
                    mapOf(
                        "creatorEarningsCoins" to (earnings - coinsAmount),
                        "lastTxId" to reqId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(reqId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun startOrGetConversation(myProfile: UserProfile, otherUser: UserProfile): Result<String> {
        return try {
            val uid = requireUserId()
            require(uid != otherUser.userId)
            val ids = listOf(uid, otherUser.userId).sorted()
            val convId = "${ids[0]}_${ids[1]}"
            val convRef = db.collection("conversations").document(convId)
            val snap = convRef.get().await()
            if (!snap.exists()) {
                convRef.set(
                    mapOf(
                        "participantIds" to ids,
                        "participantNames" to mapOf(
                            uid to myProfile.username.ifBlank { "usuario" },
                            otherUser.userId to otherUser.username.ifBlank { "creador" }
                        ),
                        "lastMessage" to "",
                        "lastSenderId" to uid,
                        "unreadUserIds" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "conversations")
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        conversation: Conversation,
        text: String,
        imageUrl: String?
    ): Result<Unit> {
        val convRef = db.collection("conversations").document(conversation.id)
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty() || !imageUrl.isNullOrBlank()) { "Escribe un mensaje o selecciona una imagen." }

            val msgRef = convRef.collection("messages").document()
            val msgPayload = buildMap<String, Any> {
                put("conversationId", conversation.id)
                put("participantIds", conversation.participantIds)
                put("senderId", uid)
                put("text", cleanText.ifBlank { "[Imagen]" })
                if (!imageUrl.isNullOrBlank()) {
                    put("imageUrl", imageUrl.trim().take(1000))
                }
                put("readBy", listOf(uid))
                put("createdAt", FieldValue.serverTimestamp())
            }

            val others = conversation.participantIds.filter { it != uid }
            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to (cleanText.ifBlank { "Imagen enviada" }).take(500),
                    "lastSenderId" to uid,
                    "unreadUserIds" to others,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(conversation: Conversation, messages: List<DirectMessage>): Result<Unit> {
        return try {
            val uid = requireUserId()
            val convRef = db.collection("conversations").document(conversation.id)
            val batch = db.batch()
            if (uid in conversation.unreadUserIds) {
                batch.update(
                    convRef,
                    mapOf(
                        "unreadUserIds" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            messages.filter { uid !in it.readBy }.take(20).forEach { msg ->
                val msgRef = convRef.collection("messages").document(msg.id)
                batch.update(msgRef, mapOf("readBy" to FieldValue.arrayUnion(uid)))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "conversations/${conversation.id}")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        var path = "reports"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reports").document()
            path = ref.path
            ref.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "reason" to reason.trim().take(100),
                    "details" to details.trim().take(500),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).collection("notifications").document(notifId)
                .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/notifications/$notifId")
            Result.failure(e)
        }
    }

    private suspend fun sendNotification(
        recipientId: String,
        actorId: String,
        actorUsername: String,
        type: String,
        title: String,
        body: String,
        referenceId: String
    ) {
        try {
            db.collection("users").document(recipientId).collection("notifications").document()
                .set(
                    mapOf(
                        "recipientId" to recipientId,
                        "actorId" to actorId,
                        "actorUsername" to actorUsername.ifBlank { "usuario" }.take(40),
                        "type" to type,
                        "title" to title.take(100),
                        "body" to body.take(300),
                        "referenceId" to referenceId.take(128),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$recipientId/notifications")
        }
    }

    // --- ADMIN / MODERATOR OPERATIONS ---

    suspend fun checkIsAdmin(uid: String, email: String?, emailVerified: Boolean): Boolean {
        if (emailVerified && email == "luismiguel30583@gmail.com") return true
        return try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeAllReportsAdmin(uid: String): Flow<List<ModerationReport>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("reports").snapshots()
                .map { snap ->
                    snap.toObjects(ModerationReport::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "reports", uid)
        )
    }

    fun observeAllClipsAdmin(uid: String): Flow<List<Clip>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("clips").snapshots()
                .map { snap ->
                    snap.toObjects(Clip::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "clips", uid)
        )
    }

    fun observeAllStreamsAdmin(uid: String): Flow<List<LiveStream>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("live_streams").snapshots()
                .map { snap ->
                    snap.toObjects(LiveStream::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "live_streams", uid)
        )
    }

    fun observeAllPurchasesAdmin(uid: String): Flow<List<CoinPurchase>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("coin_purchases").snapshots()
                .map { snap ->
                    snap.toObjects(CoinPurchase::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "coin_purchases", uid)
        )
    }

    suspend fun adminModerateClip(clipId: String, status: String, aiStatus: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("clips").document(clipId).update(
                mapOf(
                    "status" to status,
                    "aiModerationStatus" to aiStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "clips/$clipId")
            Result.failure(e)
        }
    }

    suspend fun adminTerminateStream(streamId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("live_streams").document(streamId).update(
                mapOf(
                    "status" to "terminated",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "live_streams/$streamId")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserFlags(targetUid: String, verified: Boolean, suspended: Boolean): Result<Unit> {
        return try {
            requireUserId()
            db.collection("users").document(targetUid).update(
                mapOf(
                    "verified" to verified,
                    "suspended" to suspended,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun adminResolveReport(reportId: String, status: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
            Result.failure(e)
        }
    }
}
