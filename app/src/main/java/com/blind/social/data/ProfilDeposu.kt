package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest

class ProfilDeposu {
    suspend fun profilGetir(): Result<KullaniciProfili> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Kullanıcı oturumu bulunamadı"))

            val profil = SupabaseModul.client.postgrest["profiller"]
                .select() { filter { eq("id", user.id) } }
                .decodeSingleOrNull<KullaniciProfili>()

            if (profil != null) {
                Result.success(profil)
            } else {
                // If profile doesn't exist yet, return an empty one instead of throwing an error
                Result.success(KullaniciProfili(email = user.email ?: "", kullaniciAdi = "", adSoyad = "", dogumTarihi = ""))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun profilGuncelle(profil: KullaniciProfili): Result<Unit> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Kullanıcı oturumu bulunamadı"))

            val updatedProfil = profil.copy(id = user.id)
            SupabaseModul.client.postgrest["profiller"].upsert(updatedProfil)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
