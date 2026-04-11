package com.blind.social.data

import android.content.Context
import android.media.AudioManager
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.participant.RemoteParticipant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import com.blind.social.BuildConfig

class LiveKitYonetici(private val context: Context) {

    private val wsUrl = BuildConfig.LIVEKIT_URL
    private val apiKey = BuildConfig.LIVEKIT_API_KEY
    private val apiSecret = BuildConfig.LIVEKIT_API_SECRET

    private var room: Room? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _activeSpeakers = MutableStateFlow<List<String>>(emptyList())
    val activeSpeakers: StateFlow<List<String>> = _activeSpeakers

    private val _isMicrophoneMuted = MutableStateFlow(true)
    val isMicrophoneMuted: StateFlow<Boolean> = _isMicrophoneMuted

    // Local Token Generator for MVP
    private fun generateToken(roomId: String, participantIdentity: String, participantName: String): String {
        val key = SecretKeySpec(apiSecret.toByteArray(), SignatureAlgorithm.HS256.jcaName)

        val grants = mapOf(
            "video" to mapOf(
                "room" to roomId,
                "roomJoin" to true
            )
        )

        return Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setIssuer(apiKey)
            .setSubject(participantIdentity)
            .claim("name", participantName)
            .claim("video", grants["video"])
            .setNotBefore(Date())
            .setExpiration(Date(System.currentTimeMillis() + 3600 * 1000)) // 1 saat
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    suspend fun baglan(roomId: String, participantIdentity: String, participantName: String): Result<Unit> {
        return try {
            val token = generateToken(roomId, participantIdentity, participantName)

            val audioHandler = AudioSwitchHandler(context)
            audioHandler.start()

            room = LiveKit.create(context, options = RoomOptions(adaptiveStream = true))

            coroutineScope.launch {
                room?.events?.events?.collect { event ->
                    when (event) {
                        is RoomEvent.ActiveSpeakersChanged -> {
                            _activeSpeakers.value = event.speakers.mapNotNull { (it.name ?: it.identity?.value) }
                        }
                        else -> {}
                    }
                }
            }

            room?.connect(wsUrl, token)
            // Varsayılan olarak sesi kapat
            setMicrophoneMuted(true)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun setMicrophoneMuted(muted: Boolean) {
        coroutineScope.launch {
            room?.localParticipant?.setMicrophoneEnabled(!muted)
            _isMicrophoneMuted.value = muted
        }
    }

    fun toggleSpeakerphone(useSpeaker: Boolean) {
        audioManager.isSpeakerphoneOn = useSpeaker
    }

    fun setNoiseSuppression(enabled: Boolean) {
        // Simple mock for now if noise suppression isn't explicitly exposed via RoomOptions easily in this version.
        // Usually, it's done via audio tracks or AdvancedAudioOptions.
    }

    fun ayril() {
        room?.disconnect()
        room = null
        _activeSpeakers.value = emptyList()
    }
}