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
import java.io.File
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class MesajDeposu {

    fun mesajlariDinle(odaId: String): Flow<Result<List<Mesaj>>> = flow {
        try {
            // First emit the initial state
            val initialMesajlar = SupabaseModul.client.postgrest["mesajlar"]
                .select {
                    filter {
                        eq("oda_id", odaId)
                    }
                }
                .decodeList<Mesaj>()

            val currentList = initialMesajlar.toMutableList()
            emit(Result.success(currentList.toList()))

            // Then listen for changes
            val channel = SupabaseModul.client.realtime.channel("mesajlar-changes-$odaId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>("public") {
                table = "mesajlar"
                filter = "oda_id=eq.$odaId"
            }
            channel.subscribe()

            changeFlow.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val newMesaj = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<Mesaj>(action.record)
                        currentList.add(newMesaj)
                        emit(Result.success(currentList.toList()))
                    }
                    // Handle Update/Delete similarly if needed
                    else -> {}
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun mesajGonder(odaId: String, metin: String): Result<Unit> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull() ?: return Result.failure(Exception("Not logged in"))

            val mesaj = Mesaj(
                odaId = odaId,
                gonderenId = user.id,
                metin = metin,
                mesajTipi = "metin"
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

            // 1. Storage'a yükle
            val dosyaAdi = "${user.id}_${System.currentTimeMillis()}.m4a"
            SupabaseModul.client.storage.from("sesli_mesajlar").upload(dosyaAdi, sesDosyasi.readBytes())

            // 2. URL'i al
            val url = SupabaseModul.client.storage.from("sesli_mesajlar").publicUrl(dosyaAdi)

            // 3. Mesajı veritabanına kaydet
            val mesaj = Mesaj(
                odaId = odaId,
                gonderenId = user.id,
                metin = url,
                mesajTipi = "ses"
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