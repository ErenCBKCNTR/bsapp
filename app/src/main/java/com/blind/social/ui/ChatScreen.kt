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
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    val themePreferences = remember { ThemePreferences(context) }
    val autoRead by themePreferences.autoReadMessages.collectAsState(initial = false)
    val haptic by themePreferences.hapticFeedback.collectAsState(initial = true)

    val currentUser = SupabaseModul.client.auth.currentUserOrNull()
    val isCreator = currentUser?.id == creatorId

    var selectedMessage by remember { mutableStateOf<Mesaj?>(null) }
    var showModDialog by remember { mutableStateOf(false) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Start recording can be invoked next time safely
            } else {
                android.widget.Toast.makeText(context, "Mikrofon izni gerekiyor", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun checkAndStartRecording(onStart: () -> Unit) {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> {
                onStart()
            }
            else -> {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
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

    fun stopRecordingAndSend() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            triggerVibration()

            audioFile?.let { file ->
                coroutineScope.launch {
                    val result = mesajDeposu.sesliMesajGonder(roomId, file)
                    if (result.isSuccess) {
                        file.delete()
                    }
                }
            }
            audioFile = null
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
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
        topBar = {
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
                                    mesajDeposu.mesajGonder(roomId, messageText)
                                    messageText = ""
                                    triggerVibration()
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
                                                    checkAndStartRecording {
                                                        startRecording()
                                                    }
                                                }
                                            }
                                            false -> {
                                                if (isRecording) stopRecordingAndSend()
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(mesajlar.reversed()) { mesaj ->
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
                            .semantics {
                                if (autoRead) {
                                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                                }
                                contentDescription = "${mesaj.gonderenKullaniciAdi ?: "Biri"}: ${mesaj.metin}"
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
                            Text(
                                text = mesaj.gonderenKullaniciAdi ?: "Bilinmeyen Kullanıcı",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (mesaj.mesajTipi == "ses") {
                                Text("🎵 Sesli Mesaj", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(text = mesaj.metin, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
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