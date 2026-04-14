package com.blind.social.ui

import androidx.compose.foundation.layout.*
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
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val kimlikDeposu = remember { KimlikDeposu() }
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = sifre,
                onValueChange = { sifre = it },
                label = { Text("Şifre") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
            onClick = {
                if (email.isBlank() || sifre.isBlank()) {
                    errorMessage = "E-posta ve şifre alanları boş bırakılamaz."
                    return@Button
                }
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    val result = kimlikDeposu.girisYap(email, sifre)
                    isLoading = false
                    if (result.isSuccess) {
                        onLoginSuccess()
                    } else {
                        errorMessage = "Giriş başarısız: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .semantics { contentDescription = "Giriş yapmak için çift dokunun" },
            enabled = !isLoading
        ) {
            Text("Giriş Yap")
        }

            TextButton(onClick = onNavigateToRegister) {
                Text("Hesabım Yok, Kaydol", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}