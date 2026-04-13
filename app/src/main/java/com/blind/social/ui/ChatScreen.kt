package com.blind.social.ui

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.MediaRecorder
import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.blind.social.R
import com.blind.social.SupabaseModul
import com.blind.social.data.Mesaj
import com.blind.social.data.MesajDeposu
import com.blind.social.prefs.ThemePreferences
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.ui.semantics.liveRegion
import com.blind.social.data.LiveKitYonetici

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    roomId: String,
    roomName: String,
    creatorId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val mesajDeposu = remember { MesajDeposu() }
    val coroutineScope = rememberCoroutineScope()
    var mesajlar by remember { mutableStateOf<List<Mesaj>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var localPendingMessages by remember { mutableStateOf(emptyList<Mesaj>()) }

    val themePreferences = remember { ThemePreferences(context) }
    val autoRead by themePreferences.autoReadMessages.collectAsState(initial = false)
    val haptic by themePreferences.hapticFeedback.collectAsState(initial = true)

    val currentUser = SupabaseModul.client.auth.currentUserOrNull()
    val isCreator = currentUser?.id == creatorId
    val combinedMessages = remember(mesajlar, localPendingMessages) {
        (mesajlar + localPendingMessages).sortedBy { it.olusturmaTarihi ?: "9999" }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(combinedMessages.size) {
        if (combinedMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    var selectedMessage by remember { mutableStateOf<Mesaj?>(null) }
    var showModDialog by remember { mutableStateOf(false) }

    // Audio Playback State
    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var globalMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Voice Chat State
    var showVoiceChatSheet by remember { mutableStateOf(false) }
    val liveKitYonetici = remember { LiveKitYonetici(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                pendingVoiceAction?.invoke()
                pendingVoiceAction = null
            } else {
                pendingVoiceAction = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Sesli sohbete katılmak için Mikrofon ve Ses izinleri gerekiyor.")
                }
            }
        }
    )

    fun checkPermissionsAndRun(onSuccess: () -> Unit) {
        val hasRecordAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasModifyAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED

        if (hasRecordAudio && hasModifyAudio) {
            onSuccess()
        } else {
            pendingVoiceAction = onSuccess
            multiplePermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
                )
            )
        }
    }

    fun triggerVibration() {
        if (!haptic) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(50)
        }
    }

    fun playMentionSound() {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.mention)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRecording() {
        try {
            val tempFile = File.createTempFile("sesli_mesaj", ".m4a", context.cacheDir)
            audioFile = tempFile
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(96000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(tempFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            isRecording = true
            triggerVibration()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecordingAndSend(onResult: (String?) -> Unit) {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false

            audioFile?.let { file ->
                coroutineScope.launch {
                    val result = mesajDeposu.sesliMesajGonder(roomId, file)
                    if (result.isSuccess) {
                        file.delete()
                        triggerVibration()
                        onResult(null)
                    } else {
                        val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Bilinmeyen hata"
                        val displayMsg = errorMsg.substringBefore('\n').take(60) + if (errorMsg.length > 60) "..." else ""
                        onResult("Sesli mesaj gönderilemedi: $displayMsg")
                    }
                }
            }
            audioFile = null
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            val errorMsg = e.localizedMessage ?: "Bilinmeyen hata"
            val displayMsg = errorMsg.substringBefore('\n').take(60) + if (errorMsg.length > 60) "..." else ""
            onResult("Ses kaydedilemedi: $displayMsg")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            globalMediaPlayer?.release()
        }
    }

    fun toggleAudioPlayback(url: String, messageId: String?) {
        if (playingMessageId == messageId) {
            // Stop playing
            globalMediaPlayer?.stop()
            globalMediaPlayer?.release()
            globalMediaPlayer = null
            playingMessageId = null
        } else {
            // Play new audio
            globalMediaPlayer?.release()
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(url)
                    prepareAsync()
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        release()
                        globalMediaPlayer = null
                        playingMessageId = null
                    }
                }
                globalMediaPlayer = mp
                playingMessageId = messageId
            } catch (e: Exception) {
                e.printStackTrace()
                playingMessageId = null
            }
        }
    }

    LaunchedEffect(roomId) {
        var isInitialLoad = true
        mesajDeposu.mesajlariDinle(roomId).collect { result ->
            if (result.isSuccess) {
                val yeniListe = result.getOrDefault(emptyList())
                val difference = yeniListe.size - mesajlar.size
                mesajlar = yeniListe

                if (!isInitialLoad && difference > 0) {
                    val lastMessage = yeniListe.last()
                    // Not sent by me
                    if (lastMessage.gonderenId != currentUser?.id) {
                        triggerVibration()

                        // Mention sound
                        currentUser?.userMetadata?.get("username")?.jsonPrimitive?.let { usernameObj ->
                            val username = if(usernameObj.isString) usernameObj.content else usernameObj.content
                            if (lastMessage.metin.contains("@$username")) {
                                playMentionSound()
                            }
                        }

                        if (autoRead) {
                            // Talkback announcement logic can be handled via semantics LiveRegion natively,
                            // but explicitly reading via TTS or forcing focus is tricky. We'll rely on semantics.
                        }
                    }
                }
                isInitialLoad = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(roomName)
                            Text("Katılımcılar: 1", style = MaterialTheme.typography.bodySmall) // Mock participant count
                        }
                    },
                    actions = {
                        if (isCreator) {
                            Button(onClick = { /* TODO: Close room logic */ }) {
                                Text("Odayı Kapat")
                            }
                        } else {
                            TextButton(onClick = onNavigateBack) {
                                Text("Ayrıl")
                            }
                        }
                    }
                )
                Button(
                    onClick = {
                        checkPermissionsAndRun {
                            coroutineScope.launch {
                                val username = currentUser?.userMetadata?.get("username")?.jsonPrimitive?.content ?: "Bilinmeyen"
                                val result = liveKitYonetici.baglan(roomId, currentUser?.id ?: "unknown", username)
                                if (result.isSuccess) {
                                    showVoiceChatSheet = true
                                } else {
                                    snackbarHostState.showSnackbar("Sesli sohbete bağlanırken bir hata oluştu: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Sesli Sohbete Katıl")
                }
            }
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        placeholder = { Text("Mesaj yaz...") },
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                coroutineScope.launch {
                                    val result = mesajDeposu.mesajGonder(roomId, messageText)
                                    if (result.isSuccess) {
                                        messageText = ""
                                        triggerVibration()
                                    } else {
                                        val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Bilinmeyen hata"
                                        val displayMsg = errorMsg.substringBefore('\n').take(60) + if (errorMsg.length > 60) "..." else ""
                                        snackbarHostState.showSnackbar("Mesaj gönderilemedi: $displayMsg")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Mesajı gönder" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .semantics { contentDescription = "Sesli mesaj kaydetmek için basılı tutun" }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.changes.first().pressed) {
                                            true -> {
                                                if (!isRecording) {
                                                    checkPermissionsAndRun {
                                                        startRecording()
                                                    }
                                                }
                                            }
                                            false -> {
                                                if (isRecording) {
                                                    stopRecordingAndSend { errorMsg ->
                                                        coroutineScope.launch {
                                                            if (errorMsg != null) {
                                                                snackbarHostState.showSnackbar(errorMsg)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Mikrofon", tint = if (isRecording) MaterialTheme.colorScheme.error else LocalContentColor.current)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(combinedMessages.reversed()) { mesaj ->
                val isMyMessage = mesaj.gonderenId == currentUser?.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (isCreator || isMyMessage) {
                                        selectedMessage = mesaj
                                        showModDialog = true
                                    }
                                }
                            )
                            .clearAndSetSemantics {
                                if (autoRead) {
                                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                                }
                                val senderName = mesaj.gonderenKullaniciAdi ?: mesaj.profil?.kullaniciAdi ?: "Bilinmeyen Kullanıcı"
                                if (mesaj.mesajTipi == "ses") {
                                    contentDescription = "$senderName kişisinden sesli mesaj. Oynatmak veya duraklatmak için çift dokunun."
                                    role = Role.Button
                                    onClick(label = "Oynat veya Duraklat") {
                                        toggleAudioPlayback(mesaj.metin, mesaj.id)
                                        true
                                    }
                                } else {
                                    contentDescription = "$senderName: ${mesaj.metin}"
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMyMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium.copy(
                            bottomEnd = if (isMyMessage) androidx.compose.foundation.shape.CornerSize(0.dp) else androidx.compose.foundation.shape.CornerSize(8.dp),
                            bottomStart = if (!isMyMessage) androidx.compose.foundation.shape.CornerSize(0.dp) else androidx.compose.foundation.shape.CornerSize(8.dp)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            val displaySenderName = mesaj.gonderenKullaniciAdi ?: mesaj.profil?.kullaniciAdi ?: "Bilinmeyen Kullanıcı"
                            Text(
                                text = displaySenderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (mesaj.mesajTipi == "ses") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { toggleAudioPlayback(mesaj.metin, mesaj.id) },
                                        modifier = Modifier.semantics {
                                            contentDescription = if (playingMessageId == mesaj.id) "Sesi duraklat" else "Sesi oynat"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (playingMessageId == mesaj.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null
                                        )
                                    }
                                    Text("🎵 Sesli Mesaj", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                Text(text = mesaj.metin, style = MaterialTheme.typography.bodyLarge)
                            }

                                                        val timeText = try {
                                mesaj.olusturmaTarihi?.let { dateStr ->
                                    val cleanStr = if (dateStr.contains(".")) dateStr.substringBefore(".") else dateStr.substringBefore("+").substringBefore("Z")
                                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                    parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    val date = parser.parse(cleanStr)
                                    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    formatter.timeZone = java.util.TimeZone.getDefault()
                                    formatter.format(date!!)
                                } ?: ""
                            } catch (e: Exception) {
                                ""
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (mesaj.gonderenId == currentUser?.id) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    when (mesaj.sendStatus) {
                                        "pending" -> Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Gönderilmeyi bekliyor",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        "error" -> Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Gönderilemedi",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Gönderildi",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showVoiceChatSheet) {
            var showMicSettingsDialog by remember { mutableStateOf(false) }
            var isPttMode by remember { mutableStateOf(true) } // Varsayılan Bas-Konuş
            var useSpeaker by remember { mutableStateOf(true) } // Varsayılan hoparlör
            var noiseSuppression by remember { mutableStateOf(true) } // Varsayılan açık

            ModalBottomSheet(
                onDismissRequest = {
                    showVoiceChatSheet = false
                    liveKitYonetici.ayril()
                },
                modifier = Modifier.fillMaxHeight(0.6f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sesli Sohbet Odası", style = MaterialTheme.typography.titleLarge)
                        IconButton(
                            onClick = { showMicSettingsDialog = true },
                            modifier = Modifier.semantics { contentDescription = "Ses ve mikrofon ayarları" }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val speakers by liveKitYonetici.activeSpeakers.collectAsState()

                    if (speakers.isNotEmpty()) {
                        Text(
                            text = "${speakers.first()} konuşuyor...",
                            modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                        )
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(speakers) { speaker ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(speaker)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isMuted by liveKitYonetici.isMicrophoneMuted.collectAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(120.dp)
                                .semantics {
                                    contentDescription = if (isPttMode) "Bas konuş için basılı tutun" else if (isMuted) "Sesi açmak için çift dokunun" else "Sesi kapatmak için çift dokunun"
                                }
                                .pointerInput(isPttMode) {
                                    if (isPttMode) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.changes.first().pressed) {
                                                    true -> liveKitYonetici.setMicrophoneMuted(false)
                                                    false -> liveKitYonetici.setMicrophoneMuted(true)
                                                }
                                            }
                                        }
                                    } else {
                                        detectTapGestures(
                                            onTap = {
                                                liveKitYonetici.setMicrophoneMuted(!isMuted)
                                            }
                                        )
                                    }
                                },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isMuted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.padding(32.dp),
                                tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            if (showMicSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showMicSettingsDialog = false },
                    title = { Text("Mikrofon Ayarları") },
                    text = {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Bas-Konuş Modu")
                                Switch(
                                    checked = isPttMode,
                                    onCheckedChange = { isPttMode = it },
                                    modifier = Modifier.semantics { contentDescription = "Bas konuş modunu ${if (isPttMode) "kapat" else "aç"}" }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Sesi Hoparlöre Ver")
                                Switch(
                                    checked = useSpeaker,
                                    onCheckedChange = {
                                        useSpeaker = it
                                        liveKitYonetici.toggleSpeakerphone(it)
                                    },
                                    modifier = Modifier.semantics { contentDescription = "Sesi hoparlöre vermeyi ${if (useSpeaker) "kapat" else "aç"}" }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Gürültü Engelleme")
                                Switch(
                                    checked = noiseSuppression,
                                    onCheckedChange = {
                                        noiseSuppression = it
                                        liveKitYonetici.setNoiseSuppression(it)
                                    },
                                    modifier = Modifier.semantics { contentDescription = "Gürültü engellemeyi ${if (noiseSuppression) "kapat" else "aç"}" }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMicSettingsDialog = false }) {
                            Text("Tamam")
                        }
                    }
                )
            }
        }

        if (showModDialog && selectedMessage != null) {
            AlertDialog(
                onDismissRequest = { showModDialog = false },
                title = { Text("Mesaj İşlemleri") },
                text = { Text("Bu mesaj üzerinde ne yapmak istiyorsunuz?") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                selectedMessage?.id?.let { mesajId ->
                                    mesajDeposu.mesajiSil(mesajId)
                                }
                                showModDialog = false
                            }
                        }
                    ) {
                        Text("Mesajı Sil")
                    }
                },
                dismissButton = {
                    if (isCreator && selectedMessage!!.gonderenId != currentUser?.id) {
                        TextButton(
                            onClick = {
                                android.widget.Toast.makeText(context, "${selectedMessage!!.gonderenKullaniciAdi ?: "Kullanıcı"} odadan atıldı.", android.widget.Toast.LENGTH_SHORT).show()
                                showModDialog = false
                            }
                        ) {
                            Text("Odadan At")
                        }
                    } else {
                        TextButton(onClick = { showModDialog = false }) {
                            Text("İptal")
                        }
                    }
                }
            )
        }
    }
}