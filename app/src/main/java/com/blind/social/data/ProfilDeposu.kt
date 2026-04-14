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
                // If profile doesn't exist yet, return a fallback one using user metadata
                val fallbackKullaniciAdi = user.userMetadata?.get("username")?.toString()?.replace("\"", "") ?: ""
                val fallbackAd = user.userMetadata?.get("ad")?.toString()?.replace("\"", "") ?: ""
                val fallbackSoyad = user.userMetadata?.get("soyad")?.toString()?.replace("\"", "") ?: ""
                Result.success(KullaniciProfili(
                    email = user.email ?: "",
                    kullaniciAdi = fallbackKullaniciAdi,
                    ad = fallbackAd,
                    soyad = fallbackSoyad,
                    dogumTarihi = ""
                ))
            }
        } catch (e: Exception) {
            // PostgrestException can occur if table doesn't exist or RLS blocks it.
            // Still provide a fallback instead of triggering the error UI immediately.
            val user = SupabaseModul.client.auth.currentUserOrNull()
            if (user != null) {
                val fallbackKullaniciAdi = user.userMetadata?.get("username")?.toString()?.replace("\"", "") ?: ""
                val fallbackAd = user.userMetadata?.get("ad")?.toString()?.replace("\"", "") ?: ""
                val fallbackSoyad = user.userMetadata?.get("soyad")?.toString()?.replace("\"", "") ?: ""

                Result.success(KullaniciProfili(
                    email = user.email ?: "",
                    kullaniciAdi = fallbackKullaniciAdi,
                    ad = fallbackAd,
                    soyad = fallbackSoyad,
                    dogumTarihi = ""
                ))
            } else {
                Result.failure(e)
            }
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
