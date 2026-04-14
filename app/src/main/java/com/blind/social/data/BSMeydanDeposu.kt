package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable

@Serializable
data class BegenilerRow(
    val gonderi_id: String,
    val kullanici_id: String
)

@Serializable
data class RawGonderi(
    val id: String,
    val yazar_id: String,
    val icerik: String,
    val olusturma_tarihi: String? = null,
    val created_at: String? = null,
    val begeni_sayisi: Int = 0,
    val yorum_sayisi: Int = 0,
    val profiller: KullaniciProfili? = null
)

class BSMeydanDeposu {
    // Polling logic since Realtime might be blocked by cleartext/websocket tunnel issues
    fun gonderileriGercekZamanliDinle(kullaniciId: String? = null): Flow<Result<List<Gonderi>>> = flow {
        while (true) {
            try {
                val currentUserId = SupabaseModul.client.auth.currentUserOrNull()?.id

                val rawPosts = SupabaseModul.client.postgrest["gonderiler"]
                    .select(Columns.raw("*, profiller(*)")) {
                        if (kullaniciId != null) {
                            filter { eq("yazar_id", kullaniciId) }
                        }
                    }
                    .decodeList<RawGonderi>()

                val myLikes = if (currentUserId != null) {
                    SupabaseModul.client.postgrest["begeniler"]
                        .select() { filter { eq("kullanici_id", currentUserId) } }
                        .decodeList<BegenilerRow>()
                        .map { it.gonderi_id }
                } else emptyList()

                val mappedPosts = rawPosts.map { raw ->
                    val tamAd = raw.profiller?.let { "${it.ad ?: ""} ${it.soyad ?: ""}".trim() }
                    val yazarAdi = if (!tamAd.isNullOrBlank()) tamAd else raw.profiller?.kullaniciAdi ?: "Bilinmeyen Kullanıcı"

                    // Time parsing is tricky without external libs, fallback to raw string if needed,
                    // but for now just show a simple string like "az önce" to prevent crashes.
                    val timeStr = raw.olusturma_tarihi ?: raw.created_at ?: ""

                    Gonderi(
                        id = raw.id,
                        yazarId = raw.yazar_id,
                        yazarAdiSoyadi = yazarAdi,
                        icerik = raw.icerik,
                        zamanDilimi = timeStr,
                        begeniSayisi = raw.begeni_sayisi,
                        yorumSayisi = raw.yorum_sayisi,
                        isLikedByMe = myLikes.contains(raw.id)
                    )
                }.sortedByDescending { it.zamanDilimi }

                emit(Result.success(mappedPosts))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
            delay(3000) // 3 saniye polling (Polling architecture constraint)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun gonderiOlustur(icerik: String): Result<Unit> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Oturum bulunamadı"))

            val newPost = mapOf(
                "yazar_id" to user.id,
                "icerik" to icerik
            )
            SupabaseModul.client.postgrest["gonderiler"].insert(newPost)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun gonderiBegenVeyaGeriAl(gonderiId: String, suAnBegenildiMi: Boolean): Result<Unit> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Oturum bulunamadı"))

            if (suAnBegenildiMi) {
                // Delete Like
                SupabaseModul.client.postgrest["begeniler"].delete {
                    filter {
                        eq("gonderi_id", gonderiId)
                        eq("kullanici_id", user.id)
                    }
                }
            } else {
                // Insert Like
                SupabaseModul.client.postgrest["begeniler"].insert(mapOf(
                    "gonderi_id" to gonderiId,
                    "kullanici_id" to user.id
                ))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun gonderiSil(gonderiId: String): Result<Unit> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Oturum bulunamadı"))

            SupabaseModul.client.postgrest["gonderiler"].delete {
                filter {
                    eq("id", gonderiId)
                    eq("yazar_id", user.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
