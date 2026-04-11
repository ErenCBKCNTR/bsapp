package com.blind.social.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Oda(
    val id: String = "",
    @SerialName("oda_adi")
    val odaAdi: String,
    val kapasite: Int,
    val kategori: String,
    val sifre: String? = null
)