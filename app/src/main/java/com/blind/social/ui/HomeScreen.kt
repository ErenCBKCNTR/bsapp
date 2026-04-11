package com.blind.social.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blind.social.data.KimlikDeposu
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onLogoutSuccess: () -> Unit
) {
    val kimlikDeposu = remember { KimlikDeposu() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Ana Sayfaya Hoş Geldiniz!", modifier = Modifier.padding(bottom = 24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val result = kimlikDeposu.oturumKapat()
                    if (result.isSuccess) {
                        onLogoutSuccess()
                    }
                }
            },
            modifier = Modifier.semantics { contentDescription = "Oturumu kapatmak için çift dokunun" }
        ) {
            Text("Oturumu Kapat")
        }
    }
}