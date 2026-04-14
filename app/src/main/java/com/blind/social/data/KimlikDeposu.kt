package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class KimlikDeposu {

    suspend fun kayitOl(
        email: String,
        sifre: String,
        kullaniciAdi: String,
        ad: String,
        soyad: String,
        dogumTarihi: String
    ): Result<Unit> {
        return try {
            // 1. Supabase Auth ile kayıt
            SupabaseModul.client.auth.signUpWith(Email) {
                this.email = email
                this.password = sifre

                // Trigger ve mention işlemleri için metadata
                val metadata = buildJsonObject {
                    put("username", kullaniciAdi)
                    put("ad", ad)
                    put("soyad", soyad)
                }
                this.data = metadata
            }

            // 2. Eğer doğrulama kapalıysa ve oturum otomatik açılmamışsa giriş yapmayı dene
            var user = SupabaseModul.client.auth.currentUserOrNull()
            if (user == null) {
                SupabaseModul.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = sifre
                }
                user = SupabaseModul.client.auth.currentUserOrNull()
            }

            // 3. profiller tablosuna ekle
            if (user != null) {
                val profil = KullaniciProfili(
                    id = user.id,
                    email = email,
                    kullaniciAdi = kullaniciAdi,
                    ad = ad,
                    soyad = soyad,
                    dogumTarihi = dogumTarihi
                )

                SupabaseModul.client.postgrest["profiller"].upsert(profil)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Kullanıcı oturumu başlatılamadı. Email doğrulama açık olabilir."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun girisYap(email: String, sifre: String): Result<Unit> {
        return try {
            SupabaseModul.client.auth.signInWith(Email) {
                this.email = email
                this.password = sifre
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun oturumKapat(): Result<Unit> {
        return try {
            SupabaseModul.client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sifreGuncelle(yeniSifre: String): Result<Unit> {
        return try {
            SupabaseModul.client.auth.modifyUser {
                password = yeniSifre
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}