package com.blind.social.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blind.social.prefs.ThemePreferences
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsScreen(isDesign2: Boolean = false, onToggleDesign2: ((Boolean) -> Unit)? = null) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val autoRead by themePreferences.autoReadMessages.collectAsState(initial = false)
    val haptic by themePreferences.hapticFeedback.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gelen Mesajları Otomatik Seslendir")
            Switch(
                checked = autoRead,
                onCheckedChange = {
                    coroutineScope.launch {
                        themePreferences.saveAutoReadPreference(it)
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Mesajları otomatik seslendirmeyi ${if (autoRead) "kapat" else "aç"}" }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dokunsal Geri Bildirim (Titreşim)")
            Switch(
                checked = haptic,
                onCheckedChange = {
                    coroutineScope.launch {
                        themePreferences.saveHapticPreference(it)
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Titreşimi ${if (haptic) "kapat" else "aç"}" }
            )
        }

        if (onToggleDesign2 != null) {
            Text("Tema Seçimi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tema 2 (Tasarım 2)")
                Switch(
                    checked = isDesign2,
                    onCheckedChange = {
                        onToggleDesign2(it)
                    },
                    modifier = Modifier.semantics { contentDescription = "Tema 2'ye geçişi ${if (isDesign2) "kapatıp Tema 1'e dön" else "aç"}" }
                )
            }
        }
    }
}