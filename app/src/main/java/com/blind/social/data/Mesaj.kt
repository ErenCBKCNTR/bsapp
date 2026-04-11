package com.blind.social.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Mesaj(
    val id: String? = null,
    @SerialName("oda_id")
    val odaId: String,
    @SerialName("kullanici_id")
    val gonderenId: String,
    val metin: String,
    @SerialName("mesaj_tipi")
    val mesajTipi: String = "metin", // "metin" veya "ses"
    @SerialName("olusturma_tarihi")
    val olusturmaTarihi: String? = null,
    @SerialName("gonderen_kullanici_adi")
    val gonderenKullaniciAdi: String? = null
)