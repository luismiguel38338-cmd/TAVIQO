package com.example.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.UiState
import com.example.model.AiCreatorSuggestion
import com.example.model.AiModerationResult
import com.example.ui.components.TaviqoLogoMark
import com.example.ui.components.VideoPlayerBox
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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PublishClipScreen(
    isPublishing: Boolean,
    aiSuggestionState: UiState<AiCreatorSuggestion>?,
    aiModerationState: UiState<AiModerationResult>?,
    onRequestAiSuggestions: (String) -> Unit,
    onClearAiSuggestions: () -> Unit,
    onPublishClip: (caption: String, hashtags: String, videoUrl: String, durationMs: Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var sourceMode by remember { mutableIntStateOf(0) } // 0=Galería, 1=Cámara, 2=URL HTTPS
    var selectedVideoUri by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("#Taviqo") }
    var aiTopic by remember { mutableStateOf("") }
    var useFrontCamera by remember { mutableStateOf(false) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPerm = (result[Manifest.permission.CAMERA] == true) &&
            (result[Manifest.permission.RECORD_AUDIO] == true)
    }

    // Zero-permission Photo Picker for videos (Google Play Policy compliant)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri.toString()
        }
    }

    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(aiSuggestionState) {
        if (aiSuggestionState is UiState.Success) {
            caption = aiSuggestionState.data.caption
            hashtags = aiSuggestionState.data.hashtags.joinToString(" ")
            onClearAiSuggestions()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TaviqoLogoMark(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Estudio TAVIQO • Clip 9:16",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }
        Text(
            text = "Graba con tu cámara, elige de tu galería o conecta una URL MP4/HLS real. Gemini revisa la seguridad antes de publicar.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        // Mode switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeButton(
                label = "Galería",
                icon = Icons.Default.PhotoLibrary,
                selected = sourceMode == 0,
                modifier = Modifier.weight(1f),
                onClick = { sourceMode = 0 }
            )
            ModeButton(
                label = "Grabar",
                icon = Icons.Default.Videocam,
                selected = sourceMode == 1,
                modifier = Modifier.weight(1f),
                onClick = {
                    sourceMode = 1
                    if (!hasCameraPerm) {
                        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                    }
                }
            )
            ModeButton(
                label = "URL",
                icon = Icons.Default.Link,
                selected = sourceMode == 2,
                modifier = Modifier.weight(1f),
                onClick = { sourceMode = 2 }
            )
        }

        // Source content area
        when (sourceMode) {
            0 -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RoyalBlueDark.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, RoyalBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
                            modifier = Modifier.testTag("pick_gallery_video_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seleccionar vídeo de galería")
                        }
                        if (selectedVideoUri.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Vídeo seleccionado listo para vista previa",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
            1 -> {
                if (!hasCameraPerm) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RoyalBlueDark,
                        border = BorderStroke(1.dp, RoyalBlueBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Permiso de cámara y micrófono necesario para grabar clips.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack)
                            ) {
                                Text("Permitir cámara y audio")
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianSurface)
                    ) {
                        val previewView = remember { PreviewView(context) }

                        DisposableEffect(useFrontCamera, hasCameraPerm) {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            val executor = ContextCompat.getMainExecutor(context)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val recorder = Recorder.Builder()
                                        .setQualitySelector(QualitySelector.from(Quality.HD))
                                        .build()
                                    val capture = VideoCapture.withOutput(recorder)
                                    videoCapture = capture
                                    val selector = if (useFrontCamera) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                                    cameraError = null
                                } catch (e: Exception) {
                                    cameraError = e.localizedMessage ?: "Cámara no disponible en este dispositivo"
                                }
                            }, executor)

                            onDispose {
                                activeRecording?.stop()
                                activeRecording = null
                            }
                        }

                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (cameraError != null) {
                            Text(
                                text = cameraError ?: "",
                                color = LiveCrimson,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { useFrontCamera = !useFrontCamera },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DeepBlack.copy(alpha = 0.65f))
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Cambiar cámara", tint = TextPrimary)
                            }

                            IconButton(
                                onClick = {
                                    val capture = videoCapture ?: return@IconButton
                                    val current = activeRecording
                                    if (current != null) {
                                        current.stop()
                                        activeRecording = null
                                    } else {
                                        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                            .format(System.currentTimeMillis())
                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.MediaColumns.DISPLAY_NAME, "taviqo_$name")
                                            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Taviqo")
                                            }
                                        }
                                        val outputOptions = MediaStoreOutputOptions.Builder(
                                            context.contentResolver,
                                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                        ).setContentValues(contentValues).build()

                                        val recording = capture.output
                                            .prepareRecording(context, outputOptions)
                                            .apply {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                    withAudioEnabled()
                                                }
                                            }
                                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                                if (event is VideoRecordEvent.Finalize && !event.hasError()) {
                                                    selectedVideoUri = event.outputResults.outputUri.toString()
                                                }
                                            }
                                        activeRecording = recording
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (activeRecording != null) LiveCrimson else NeonCyan)
                            ) {
                                Icon(
                                    imageVector = if (activeRecording != null) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = if (activeRecording != null) "Detener grabación" else "Grabar",
                                    tint = DeepBlack,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                OutlinedTextField(
                    value = selectedVideoUri,
                    onValueChange = { selectedVideoUri = it },
                    label = { Text("URL pública del vídeo (MP4 / HLS)") },
                    placeholder = { Text("https://...", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = RoyalBlueBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (selectedVideoUri.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                VideoPlayerBox(
                    videoUrl = selectedVideoUri,
                    isActive = true,
                    isMuted = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Gemini Creator Assistant for Caption & Hashtags
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = RoyalBlueElevated.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Asistente Gemini AI (Descripción y Hashtags)", style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = aiTopic,
                        onValueChange = { aiTopic = it },
                        placeholder = { Text("Tema del vídeo (ej. freestyle, setup gamer)...", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { onRequestAiSuggestions(aiTopic) },
                        enabled = aiSuggestionState !is UiState.Loading
                    ) {
                        if (aiSuggestionState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sugerir", color = NeonCyan)
                        }
                    }
                }
                if (aiSuggestionState is UiState.Error) {
                    Text(aiSuggestionState.message, color = LiveCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Descripción del vídeo") },
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = RoyalBlueBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_caption_input")
        )

        OutlinedTextField(
            value = hashtags,
            onValueChange = { hashtags = it },
            label = { Text("Hashtags (separados por espacio)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = RoyalBlueBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (aiModerationState is UiState.Success) {
            val res = aiModerationState.data
            Text(
                text = "Revisión IA (${res.riskLevel}): ${res.reason}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (res.safe) EmeraldSuccess else LiveCrimson
            )
        }

        Button(
            onClick = { onPublishClip(caption, hashtags, selectedVideoUri, 15_000L) },
            enabled = !isPublishing && selectedVideoUri.isNotBlank() && caption.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("publish_clip_button")
        ) {
            if (isPublishing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DeepBlack, strokeWidth = 2.5.dp)
            } else {
                Icon(Icons.Default.Publish, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publicar Clip Real", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) NeonCyan else RoyalBlueDark,
        border = if (selected) null else BorderStroke(1.dp, RoyalBlueBorder),
        modifier = modifier.height(48.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) DeepBlack else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) DeepBlack else TextPrimary
            )
        }
    }
}
