package com.blind.social.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.blind.social.data.KimlikDeposu
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val kimlikDeposu = remember { KimlikDeposu() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var ad by remember { mutableStateOf("") }
    var soyad by remember { mutableStateOf("") }
    var kullaniciAdi by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var dogumTarihi by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .semantics {
                        liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                    }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = ad,
                onValueChange = { if (it.length <= 50) ad = it },
                label = { Text("Ad") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Adınızı girin, en fazla 50 karakter" },
                singleLine = true
            )
            OutlinedTextField(
                value = soyad,
                onValueChange = { if (it.length <= 50) soyad = it },
                label = { Text("Soyad") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Soyadınızı girin, en fazla 50 karakter" },
                singleLine = true
            )
        }

        OutlinedTextField(
            value = kullaniciAdi,
            onValueChange = { if (it.length <= 30) kullaniciAdi = it },
            label = { Text("Kullanıcı Adı") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .semantics { contentDescription = "Kullanıcı adınızı girin, en fazla 30 karakter" },
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { if (it.length <= 254) email = it },
            label = { Text("E-posta") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .semantics { contentDescription = "E-posta adresinizi girin, en fazla 254 karakter" },
            singleLine = true
        )

        OutlinedTextField(
            value = sifre,
            onValueChange = { if (it.length <= 64) sifre = it },
            label = { Text("Şifre") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .semantics { contentDescription = "Şifrenizi girin, en az 6, en fazla 64 karakter" },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        val dogumTarihiFormated = if (dogumTarihi.length == 8) {
            "${dogumTarihi.substring(0, 2)}.${dogumTarihi.substring(2, 4)}.${dogumTarihi.substring(4, 8)}"
        } else dogumTarihi

        OutlinedTextField(
            value = dogumTarihi,
            onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) dogumTarihi = it },
            label = { Text("Doğum Tarihi") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .semantics { contentDescription = "Doğum Tarihi. Şu anki değer: $dogumTarihiFormated" },
            singleLine = true,
            visualTransformation = DateVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        Button(
            onClick = {
                if (ad.isBlank() || soyad.isBlank() || kullaniciAdi.isBlank() || email.isBlank() || sifre.isBlank() || dogumTarihi.isBlank()) {
                    errorMessage = "Lütfen tüm alanları doldurun."
                    return@Button
                }
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    val result = kimlikDeposu.kayitOl(
                        email = email,
                        sifre = sifre,
                        kullaniciAdi = kullaniciAdi,
                        ad = ad,
                        soyad = soyad,
                        dogumTarihi = dogumTarihi
                    )
                    isLoading = false
                    if (result.isSuccess) {
                        onRegisterSuccess()
                    } else {
                        errorMessage = "Kayıt başarısız: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .semantics { contentDescription = "Kayıt olmak için çift dokunun" },
            enabled = !isLoading
        ) {
            Text("Kayıt Ol")
        }

            TextButton(onClick = onNavigateToLogin) {
                Text("Zaten hesabım var, Giriş Yap", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}