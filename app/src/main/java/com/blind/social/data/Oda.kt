package com.blind.social.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Oda(
    val id: String? = null,
    @SerialName("oda_adi")
    val odaAdi: String,
    val kapasite: Int,
    val kategori: String,
    val sifre: String? = null,
    @SerialName("kurucu_id")
    val kurucuId: String? = null
)