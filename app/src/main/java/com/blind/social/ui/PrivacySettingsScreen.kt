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
fun PrivacySettingsScreen() {
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
    }
}