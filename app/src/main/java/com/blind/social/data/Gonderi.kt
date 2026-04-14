package com.blind.social.data

data class Gonderi(
    val id: String,
    val yazarId: String,
    val yazarAdiSoyadi: String,
    val icerik: String,
    val zamanDilimi: String,
    val begeniSayisi: Int,
    val yorumSayisi: Int,
    val isLikedByMe: Boolean = false
)
