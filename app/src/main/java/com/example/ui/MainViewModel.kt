package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.core.unwrapFirestoreError
import com.example.data.GeminiAiRepository
import com.example.data.LoboRepository
import com.example.data.PreferencesRepository
import com.example.model.AiCreatorSuggestion
import com.example.model.AiModerationResult
import com.example.model.Clip
import com.example.model.ClipComment
import com.example.model.CoinPackage
import com.example.model.CoinPurchase
import com.example.model.Conversation
import com.example.model.DirectMessage
import com.example.model.GiftBurstEvent
import com.example.model.GiftCatalogItem
import com.example.model.GiftTransaction
import com.example.model.LiveChatMessage
import com.example.model.LiveStream
import com.example.model.TaviqoCatalog
import com.example.model.ModerationReport
import com.example.model.PayoutRequest
import com.example.model.PrivateAccount
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: LoboRepository,
    private val aiRepository: GeminiAiRepository,
    private val prefsRepository: PreferencesRepository,
    private val firebaseUser: FirebaseUser
) : ViewModel() {

    val currentUserId: String = firebaseUser.uid

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage = _toastMessage.asSharedFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _activeGiftBurst = MutableStateFlow<GiftBurstEvent?>(null)
    val activeGiftBurst: StateFlow<GiftBurstEvent?> = _activeGiftBurst.asStateFlow()

    private val _recommendedOrder = MutableStateFlow<List<String>>(emptyList())
    val recommendedOrder: StateFlow<List<String>> = _recommendedOrder.asStateFlow()

    private val _aiSuggestionState = MutableStateFlow<UiState<AiCreatorSuggestion>?>(null)
    val aiSuggestionState: StateFlow<UiState<AiCreatorSuggestion>?> = _aiSuggestionState.asStateFlow()

    private val _aiModerationState = MutableStateFlow<UiState<AiModerationResult>?>(null)
    val aiModerationState: StateFlow<UiState<AiModerationResult>?> = _aiModerationState.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<Pair<Boolean, String>>>(emptyList())
    val aiChatMessages: StateFlow<List<Pair<Boolean, String>>> = _aiChatMessages.asStateFlow()

    private val _aiChatStreaming = MutableStateFlow<String?>(null)
    val aiChatStreaming: StateFlow<String?> = _aiChatStreaming.asStateFlow()

    private val _aiChatError = MutableStateFlow<String?>(null)
    val aiChatError: StateFlow<String?> = _aiChatError.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    val isMuted: StateFlow<Boolean> = prefsRepository.isMutedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val isAutoPlay: StateFlow<Boolean> = prefsRepository.isAutoPlayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val myProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeMyProfile(currentUserId).toUiState("perfil")

    val myWalletState: StateFlow<UiState<PrivateAccount?>> =
        repository.observeMyWallet(currentUserId).toUiState("billetera")

    val creatorsState: StateFlow<UiState<List<UserProfile>>> =
        repository.observeCreators(currentUserId).toUiState("creadores")

    val activeClipsState: StateFlow<UiState<List<Clip>>> =
        repository.observeActiveClips(currentUserId).toUiState("clips")

    val followingIds: StateFlow<Set<String>> =
        repository.observeFollowingIds(currentUserId)
            .catch { error -> unwrapAndLog("following", error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val favoriteIds: StateFlow<Set<String>> =
        repository.observeFavoriteIds(currentUserId)
            .catch { error -> unwrapAndLog("favorites", error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val notificationsState: StateFlow<UiState<List<UserNotification>>> =
        repository.observeNotifications(currentUserId).toUiState("notificaciones")

    val liveStreamsState: StateFlow<UiState<List<LiveStream>>> =
        repository.observeLiveStreams(currentUserId).toUiState("directos")

    val giftHistoryState: StateFlow<UiState<List<GiftTransaction>>> =
        repository.observeGiftHistory(currentUserId).toUiState("historial de regalos")

    val coinPurchasesState: StateFlow<UiState<List<CoinPurchase>>> =
        repository.observeCoinPurchases(currentUserId).toUiState("compras")

    val payoutRequestsState: StateFlow<UiState<List<PayoutRequest>>> =
        repository.observePayoutRequests(currentUserId).toUiState("retiros")

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId).toUiState("conversaciones")

    val adminReportsState: StateFlow<UiState<List<ModerationReport>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllReportsAdmin(currentUserId).map<List<ModerationReport>, UiState<List<ModerationReport>>> { UiState.Success(it) }
            else flowOf(UiState.Success(emptyList()))
        }.catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.message ?: "Error al cargar denuncias"))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminClipsState: StateFlow<UiState<List<Clip>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllClipsAdmin(currentUserId).map<List<Clip>, UiState<List<Clip>>> { UiState.Success(it) }
            else flowOf(UiState.Success(emptyList()))
        }.catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.message ?: "Error al cargar clips"))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminStreamsState: StateFlow<UiState<List<LiveStream>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllStreamsAdmin(currentUserId).map<List<LiveStream>, UiState<List<LiveStream>>> { UiState.Success(it) }
            else flowOf(UiState.Success(emptyList()))
        }.catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.message ?: "Error al cargar directos"))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminPurchasesState: StateFlow<UiState<List<CoinPurchase>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllPurchasesAdmin(currentUserId).map<List<CoinPurchase>, UiState<List<CoinPurchase>>> { UiState.Success(it) }
            else flowOf(UiState.Success(emptyList()))
        }.catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.message ?: "Error al cargar transacciones"))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val viewedClipsThisSession = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.ensureUserBootstrap(firebaseUser).onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al inicializar cuenta")
            }
            _isAdmin.value = repository.checkIsAdmin(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                emailVerified = firebaseUser.isEmailVerified
            )
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Error al cargar $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    }

    private fun unwrapAndLog(label: String, error: Throwable) {
        val root = unwrapFirestoreError(error)
        Log.w(TAG, "Stream error on $label", root)
    }

    fun observeClipComments(clipId: String): Flow<List<ClipComment>> {
        return repository.observeClipComments(clipId, currentUserId)
            .catch { error -> unwrapAndLog("comments", error) }
    }

    fun observeLiveChat(streamId: String): Flow<List<LiveChatMessage>> {
        return repository.observeLiveChat(streamId, currentUserId)
            .catch { error -> unwrapAndLog("live_chat", error) }
    }

    fun observeDirectMessages(conversationId: String): Flow<List<DirectMessage>> {
        return repository.observeDirectMessages(conversationId, currentUserId)
            .catch { error -> unwrapAndLog("messages", error) }
    }

    fun toggleMuted() {
        viewModelScope.launch { prefsRepository.setMuted(!isMuted.value) }
    }

    fun toggleAutoPlay() {
        viewModelScope.launch { prefsRepository.setAutoPlay(!isAutoPlay.value) }
    }

    fun recordViewIfNeeded(clipId: String) {
        if (viewedClipsThisSession.add(clipId)) {
            viewModelScope.launch { repository.incrementClipView(clipId) }
        }
    }

    fun toggleLike(clip: Clip) {
        viewModelScope.launch {
            repository.toggleLikeClip(clip).onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "No se pudo actualizar Me gusta")
            }
        }
    }

    fun toggleFavorite(clipId: String) {
        viewModelScope.launch {
            repository.toggleFavoriteClip(clipId).onSuccess { saved ->
                _toastMessage.tryEmit(if (saved) "Guardado en favoritos" else "Eliminado de favoritos")
            }.onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "Error al guardar favorito")
            }
        }
    }

    fun recordShare(clipId: String) {
        viewModelScope.launch { repository.incrementClipShare(clipId) }
    }

    fun addComment(clip: Clip, text: String) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val mod = aiRepository.moderateContent(text).getOrNull()
            if (mod != null && !mod.safe && mod.riskLevel == "high") {
                _toastMessage.tryEmit("Comentario bloqueado por moderación IA: ${mod.reason}")
                return@launch
            }
            repository.addComment(profile, clip, text).onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "Error al comentar")
            }
        }
    }

    fun toggleFollow(targetUser: UserProfile) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleFollow(myProfile, targetUser).onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "No se pudo actualizar seguimiento")
            }
        }
    }

    fun toggleBlockUser(targetUid: String, block: Boolean) {
        viewModelScope.launch {
            repository.toggleBlockUser(targetUid, block).onSuccess {
                _toastMessage.tryEmit(if (block) "Usuario bloqueado" else "Usuario desbloqueado")
            }.onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "No se pudo actualizar bloqueo")
            }
        }
    }

    fun updateProfile(username: String, displayName: String, bio: String, avatarUrl: String?) {
        viewModelScope.launch {
            repository.updateProfile(username, displayName, bio, avatarUrl).onSuccess {
                _toastMessage.tryEmit("Perfil actualizado")
            }.onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "Error al actualizar perfil")
            }
        }
    }

    fun requestAiSuggestions(topic: String) {
        if (topic.isBlank()) {
            _toastMessage.tryEmit("Escribe un tema o idea para generar sugerencias")
            return
        }
        viewModelScope.launch {
            _aiSuggestionState.value = UiState.Loading
            aiRepository.suggestCaptionAndHashtags(topic)
                .onSuccess { _aiSuggestionState.value = UiState.Success(it) }
                .onFailure { err ->
                    val msg = if (err is TimeoutCancellationException) {
                        "Gemini tardó demasiado. Intenta de nuevo."
                    } else {
                        err.localizedMessage ?: "Error al consultar Gemini"
                    }
                    _aiSuggestionState.value = UiState.Error(msg)
                }
        }
    }

    fun clearAiSuggestions() {
        _aiSuggestionState.value = null
    }

    fun publishClip(
        caption: String,
        hashtagsRaw: String,
        videoUrl: String,
        durationMs: Long,
        onPublished: () -> Unit
    ) {
        val profile = (myProfileState.value as? UiState.Success)?.data
        if (profile == null) {
            _toastMessage.tryEmit("Perfil aún cargando, intenta en un segundo")
            return
        }
        val hashtags = hashtagsRaw
            .split(",", " ", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("#")) it else "#$it" }
            .distinct()
            .take(10)

        viewModelScope.launch {
            _isPublishing.value = true
            _aiModerationState.value = UiState.Loading
            val modResult = aiRepository.moderateContent("$caption ${hashtags.joinToString(" ")}").getOrNull()
            val aiStatus = when {
                modResult == null -> "review"
                !modResult.safe || modResult.suggestedAction == "reject" -> {
                    _aiModerationState.value = UiState.Success(modResult)
                    _isPublishing.value = false
                    _toastMessage.tryEmit("Contenido rechazado por moderación IA: ${modResult.reason}")
                    return@launch
                }
                modResult.suggestedAction == "review" -> "review"
                else -> "approved"
            }
            if (modResult != null) {
                _aiModerationState.value = UiState.Success(modResult)
            }
            repository.publishClip(
                myProfile = profile,
                caption = caption,
                hashtags = hashtags,
                videoUrl = videoUrl,
                durationMs = durationMs,
                aiModerationStatus = aiStatus
            ).onSuccess {
                _isPublishing.value = false
                _toastMessage.tryEmit("Clip publicado correctamente")
                onPublished()
            }.onFailure { err ->
                _isPublishing.value = false
                _toastMessage.tryEmit(err.localizedMessage ?: "No se pudo publicar el clip")
            }
        }
    }

    fun refreshRecommendationsFromRealActivity() {
        val clips = (activeClipsState.value as? UiState.Success)?.data ?: return
        val favs = favoriteIds.value
        val likedTags = clips.filter { it.id in favs }.flatMap { it.hashtags }.distinct()
        viewModelScope.launch {
            aiRepository.rankRecommendedClips(likedTags, clips).onSuccess { ordered ->
                _recommendedOrder.value = ordered
                _toastMessage.tryEmit("Recomendaciones actualizadas según tu actividad real")
            }.onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "No se pudo actualizar ranking IA")
            }
        }
    }

    fun sendVirtualGift(
        recipientId: String,
        recipientUsername: String,
        gift: GiftCatalogItem,
        contextType: String,
        contextId: String
    ) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendVirtualGift(
                senderProfile = profile,
                recipientId = recipientId,
                recipientUsername = recipientUsername,
                gift = gift,
                contextType = contextType,
                contextId = contextId
            ).onSuccess {
                val burst = GiftBurstEvent(
                    id = it.id,
                    gift = gift,
                    senderUsername = profile.username,
                    recipientUsername = recipientUsername
                )
                _activeGiftBurst.value = burst
                _toastMessage.tryEmit("¡Enviaste ${gift.name} a @$recipientUsername!")
                delay(3200L)
                if (_activeGiftBurst.value?.id == burst.id) {
                    _activeGiftBurst.value = null
                }
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al enviar regalo")
            }
        }
    }

    fun dismissGiftBurst() {
        _activeGiftBurst.value = null
    }

    fun createCoinPurchaseOrder(pkg: CoinPackage, provider: String) {
        viewModelScope.launch {
            repository.createCoinPurchaseOrder(pkg, provider).onSuccess { orderId ->
                _toastMessage.tryEmit(
                    "Orden #$orderId registrada como pendiente de verificación del servidor (Google Play Billing / Cloud Functions)."
                )
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al crear orden")
            }
        }
    }

    fun requestPayout(coinsAmount: Long, provider: String, destinationHint: String) {
        viewModelScope.launch {
            repository.requestCreatorPayout(coinsAmount, provider, destinationHint).onSuccess {
                _toastMessage.tryEmit("Solicitud de retiro enviada para validación administrativa")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al solicitar retiro")
            }
        }
    }

    fun startLiveStream(title: String, category: String, streamUrl: String, onCreated: (String) -> Unit) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val mod = aiRepository.moderateContent(title).getOrNull()
            if (mod != null && !mod.safe && mod.riskLevel == "high") {
                _toastMessage.tryEmit("Título bloqueado por moderación IA: ${mod.reason}")
                return@launch
            }
            repository.startLiveStream(profile, title, category, streamUrl)
                .onSuccess { streamId -> onCreated(streamId) }
                .onFailure { err -> _toastMessage.tryEmit(err.localizedMessage ?: "No se pudo iniciar directo") }
        }
    }

    fun endLiveStream(streamId: String) {
        viewModelScope.launch {
            repository.endLiveStream(streamId).onSuccess {
                _toastMessage.tryEmit("Transmisión finalizada")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al finalizar directo")
            }
        }
    }

    fun joinLiveStream(streamId: String) {
        viewModelScope.launch { repository.joinLiveStream(streamId) }
    }

    fun leaveLiveStream(streamId: String) {
        viewModelScope.launch { repository.leaveLiveStream(streamId) }
    }

    fun sendLiveChat(streamId: String, text: String) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendLiveChat(profile, streamId, text).onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al enviar mensaje")
            }
        }
    }

    fun openOrCreateConversation(otherUser: UserProfile, onReady: (String) -> Unit) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.startOrGetConversation(profile, otherUser)
                .onSuccess(onReady)
                .onFailure { err -> _toastMessage.tryEmit(err.localizedMessage ?: "No se pudo abrir el chat") }
        }
    }

    fun sendDirectMessage(conversation: Conversation, text: String, imageUrl: String?) {
        viewModelScope.launch {
            repository.sendDirectMessage(conversation, text, imageUrl).onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "No se pudo enviar mensaje")
            }
        }
    }

    fun markConversationRead(conversation: Conversation, messages: List<DirectMessage>) {
        viewModelScope.launch { repository.markConversationRead(conversation, messages) }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { repository.markNotificationRead(notifId) }
    }

    fun submitReport(targetType: String, targetId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(targetType, targetId, reason, details).onSuccess {
                _toastMessage.tryEmit("Denuncia enviada al equipo de moderación")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "No se pudo enviar la denuncia")
            }
        }
    }

    fun sendAiAssistantPrompt(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        _aiChatMessages.value = _aiChatMessages.value + (true to clean)
        _aiChatError.value = null
        _aiChatStreaming.value = ""
        viewModelScope.launch {
            try {
                var finalAnswer = ""
                aiRepository.askAssistantStream(clean).collect { accumulated ->
                    finalAnswer = accumulated
                    _aiChatStreaming.value = accumulated
                }
                _aiChatStreaming.value = null
                _aiChatMessages.value = _aiChatMessages.value + (false to finalAnswer)
            } catch (e: TimeoutCancellationException) {
                _aiChatStreaming.value = null
                _aiChatError.value = "Tiempo de espera agotado (60s). Intenta de nuevo."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiChatStreaming.value = null
                _aiChatError.value = e.localizedMessage ?: "Error al comunicarse con Gemini AI."
            }
        }
    }

    // --- ADMIN ACTIONS ---
    fun adminModerateClip(clipId: String, status: String) {
        viewModelScope.launch {
            val aiStatus = if (status == "active") "approved" else "rejected"
            repository.adminModerateClip(clipId, status, aiStatus).onSuccess {
                _toastMessage.tryEmit("Estado del clip actualizado a $status")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error de moderación")
            }
        }
    }

    fun adminTerminateStream(streamId: String) {
        viewModelScope.launch {
            repository.adminTerminateStream(streamId).onSuccess {
                _toastMessage.tryEmit("Directo terminado por administración")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al terminar directo")
            }
        }
    }

    fun adminUpdateUserFlags(targetUid: String, verified: Boolean, suspended: Boolean) {
        viewModelScope.launch {
            repository.adminUpdateUserFlags(targetUid, verified, suspended).onSuccess {
                _toastMessage.tryEmit("Usuario actualizado")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al actualizar usuario")
            }
        }
    }

    fun adminResolveReport(reportId: String, status: String) {
        viewModelScope.launch {
            repository.adminResolveReport(reportId, status).onSuccess {
                _toastMessage.tryEmit("Denuncia marcada como $status")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Error al actualizar denuncia")
            }
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
