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

    var adSoyad by remember { mutableStateOf("") }
    var kullaniciAdi by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var dogumTarihi by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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

        TextField(
            value = adSoyad,
            onValueChange = { adSoyad = it },
            label = { Text("Ad Soyad") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        TextField(
            value = kullaniciAdi,
            onValueChange = { kullaniciAdi = it },
            label = { Text("Kullanıcı Adı") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        TextField(
            value = sifre,
            onValueChange = { sifre = it },
            label = { Text("Şifre") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        TextField(
            value = dogumTarihi,
            onValueChange = { dogumTarihi = it },
            label = { Text("Doğum Tarihi (GG.AA.YYYY)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )

        Button(
            onClick = {
                if (adSoyad.isBlank() || kullaniciAdi.isBlank() || email.isBlank() || sifre.isBlank() || dogumTarihi.isBlank()) {
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
                        adSoyad = adSoyad,
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
            Text("Zaten hesabım var, Giriş Yap")
        }
    }
}