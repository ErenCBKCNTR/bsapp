package com.blind.social.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class KullaniciProfili(
    val id: String = "",
    val email: String,
    @SerialName("kullanici_adi")
    val kullaniciAdi: String,
    @SerialName("ad_soyad")
    val adSoyad: String,
    @SerialName("dogum_tarihi")
    val dogumTarihi: String
)