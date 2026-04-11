package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import io.github.jan.supabase.postgrest.query.Columns

class MesajDeposu {

    fun mesajlariDinle(odaId: String): Flow<Result<List<Mesaj>>> = kotlinx.coroutines.flow.callbackFlow {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        var currentList = mutableListOf<Mesaj>()

        try {
            // First fetch initial state
            val initialMesajlar = SupabaseModul.client.postgrest["mesajlar"]
                .select(columns = Columns.raw("*, profiller(kullanici_adi)")) {
                    filter {
                        eq("oda_id", odaId)
                    }
                }
                .decodeList<Mesaj>()

            currentList = initialMesajlar.toMutableList()
            trySend(Result.success(currentList.toList()))

            // Then subscribe to realtime changes
            val channel = SupabaseModul.client.realtime.channel("mesajlar-changes-$odaId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>("public") {
                table = "mesajlar"
                filter = "oda_id=eq.$odaId"
            }
            channel.subscribe()

            val job = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                changeFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            val newMesaj = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<Mesaj>(action.record)
                            currentList.add(newMesaj)
                            trySend(Result.success(currentList.toList()))
                        }
                        is PostgresAction.Delete -> {
                            val deletedId = action.oldRecord["id"]?.jsonPrimitive?.content
                            if (deletedId != null) {
                                currentList.removeAll { it.id == deletedId }
                                trySend(Result.success(currentList.toList()))
                            }
                        }
                        else -> {}
                    }
                }
            }

            awaitClose {
                job.cancel()
                coroutineScope.launch { channel.unsubscribe() }
            }
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun mesajGonder(odaId: String, metin: String): Result<Unit> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull() ?: return Result.failure(Exception("Not logged in"))
            val username = user.userMetadata?.get("username")?.jsonPrimitive?.content ?: "Bilinmeyen Kullanıcı"

            val mesaj = Mesaj(
                odaId = odaId,
                gonderenId = user.id,
                metin = metin,
                mesajTipi = "metin",
                gonderenKullaniciAdi = username
            )
            SupabaseModul.client.postgrest["mesajlar"].insert(mesaj)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MesajDeposu", "Mesaj Gönder Hatası", e)
            Result.failure(e)
        }
    }

    suspend fun sesliMesajGonder(odaId: String, sesDosyasi: File): Result<Unit> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull() ?: return Result.failure(Exception("Not logged in"))
            val username = user.userMetadata?.get("username")?.jsonPrimitive?.content ?: "Bilinmeyen Kullanıcı"

            // 1. Storage'a yükle
            val dosyaAdi = "${user.id}_${System.currentTimeMillis()}.m4a"
            val bucketName = "sesli_mesajlar"
            SupabaseModul.client.storage.from(bucketName).upload(dosyaAdi, sesDosyasi.readBytes())

            // 2. URL'i al
            val url = SupabaseModul.client.storage.from(bucketName).publicUrl(dosyaAdi)

            // 3. Mesajı veritabanına kaydet
            val mesaj = Mesaj(
                odaId = odaId,
                gonderenId = user.id,
                metin = url,
                mesajTipi = "ses",
                gonderenKullaniciAdi = username
            )
            SupabaseModul.client.postgrest["mesajlar"].insert(mesaj)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MesajDeposu", "Sesli Mesaj Hatası", e)
            Result.failure(e)
        }
    }

    suspend fun mesajiSil(mesajId: String): Result<Unit> {
        return try {
            SupabaseModul.client.postgrest["mesajlar"].delete {
                filter {
                    eq("id", mesajId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}