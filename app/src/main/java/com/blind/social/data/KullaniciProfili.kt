package com.blind.social.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class KullaniciProfili(
    val id: String? = null,
    val email: String,
    @SerialName("kullanici_adi")
    val kullaniciAdi: String,
    @SerialName("ad")
    val ad: String? = null,
    @SerialName("soyad")
    val soyad: String? = null,
    @SerialName("dogum_tarihi")
    val dogumTarihi: String,
    @SerialName("hakkimda")
    val hakkimda: String? = null,
    @SerialName("baglantilar")
    val baglantilar: String? = null
)