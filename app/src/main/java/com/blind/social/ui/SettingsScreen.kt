package com.blind.social.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import com.blind.social.prefs.ThemePreferences
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    isDesign2: Boolean = false,
    onToggleDesign2: ((Boolean) -> Unit)? = null,
    isDarkMode: Boolean = false,
    onToggleTheme: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val autoRead by themePreferences.autoReadMessages.collectAsState(initial = false)
    val haptic by themePreferences.hapticFeedback.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Local states for UI (Backend placeholders)
    var readSpeed by remember { mutableStateOf(1f) } // 0f = Normal, 1f = Hızlı, 2f = Çok Hızlı
    var shakeToListen by remember { mutableStateOf(false) }

    var highContrast by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(1f) }

    var hideOnlineStatus by remember { mutableStateOf(false) }
    var hideProfile by remember { mutableStateOf(false) }

    var autoPlayVoiceMessages by remember { mutableStateOf(true) }
    var roomEntrySounds by remember { mutableStateOf(true) }
    var mentionNotifications by remember { mutableStateOf(true) }
    var dataSaverMode by remember { mutableStateOf(false) }

    var expandedInviteDropdown by remember { mutableStateOf(false) }
    var selectedInviteRule by remember { mutableStateOf("Herkes") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Uygulama Ayarları", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

        // A. Erişilebilirlik Ayarları
        SettingsSectionHeader("A. Erişilebilirlik Ayarları")
        SettingsSwitchRow(
            title = "Gelen Mesajları Otomatik Seslendir",
            checked = autoRead,
            onCheckedChange = { coroutineScope.launch { themePreferences.saveAutoReadPreference(it) } },
            desc = "Mesajları otomatik seslendirmeyi"
        )
        SettingsSwitchRow(
            title = "Dokunsal Geri Bildirim (Titreşim)",
            checked = haptic,
            onCheckedChange = { coroutineScope.launch { themePreferences.saveHapticPreference(it) } },
            desc = "Titreşimi"
        )
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            val speedLabel = when (readSpeed) {
                0f -> "Normal"
                1f -> "Hızlı"
                else -> "Çok Hızlı"
            }
            Text("Okuma Hızı: $speedLabel", modifier = Modifier.padding(bottom = 8.dp))
            Slider(
                value = readSpeed,
                onValueChange = { readSpeed = it },
                valueRange = 0f..2f,
                steps = 1,
                modifier = Modifier.semantics { contentDescription = "Okuma Hızı Ayarı. Mevcut: $speedLabel" }
            )
        }
        SettingsSwitchRow(
            title = "Salla ve Dinle",
            checked = shakeToListen,
            onCheckedChange = { shakeToListen = it },
            desc = "Salla ve Dinle özelliğini"
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // B. Tema ve Görünüm
        SettingsSectionHeader("B. Tema ve Görünüm")
        if (onToggleTheme != null) {
            SettingsSwitchRow(
                title = "Karanlık Mod",
                checked = isDarkMode,
                onCheckedChange = { onToggleTheme(it) },
                desc = "Karanlık modu"
            )
        }
        if (onToggleDesign2 != null) {
            SettingsSwitchRow(
                title = "Tasarım 2 (Yeni Tema)",
                checked = isDesign2,
                onCheckedChange = { onToggleDesign2(it) },
                desc = "Tasarım 2 temasını"
            )
        }
        SettingsSwitchRow(
            title = "Yüksek Karşıtlık Modu",
            checked = highContrast,
            onCheckedChange = { highContrast = it },
            desc = "Yüksek karşıtlık modunu"
        )
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Yazı Tipi Boyutu: ${fontSize.toInt()}x", modifier = Modifier.padding(bottom = 8.dp))
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 1f..3f,
                steps = 1,
                modifier = Modifier.semantics { contentDescription = "Yazı Tipi Boyutu. Mevcut: ${fontSize.toInt()}x" }
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // C. Gizlilik ve Güvenlik
        SettingsSectionHeader("C. Gizlilik ve Güvenlik")
        SettingsSwitchRow(
            title = "Çevrimiçi Durumumu Gizle",
            checked = hideOnlineStatus,
            onCheckedChange = { hideOnlineStatus = it },
            desc = "Çevrimiçi durumu gizlemeyi"
        )
        SettingsSwitchRow(
            title = "Profilimi Gizle",
            checked = hideProfile,
            onCheckedChange = { hideProfile = it },
            desc = "Profil gizlemeyi"
        )

        // Davet Sınırı Dropdown
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Oda Davetlerini Sınırla", modifier = Modifier.padding(bottom = 8.dp))
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expandedInviteDropdown,
                onExpandedChange = { expandedInviteDropdown = !expandedInviteDropdown }
            ) {
                TextField(
                    readOnly = true,
                    value = selectedInviteRule,
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInviteDropdown) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor().semantics { contentDescription = "Oda daveti kuralını seçin. Mevcut: $selectedInviteRule" }
                )
                ExposedDropdownMenu(
                    expanded = expandedInviteDropdown,
                    onDismissRequest = { expandedInviteDropdown = false }
                ) {
                    listOf("Herkes", "Sadece Takipçiler").forEach { rule ->
                        DropdownMenuItem(
                            text = { Text(rule) },
                            onClick = {
                                selectedInviteRule = rule
                                expandedInviteDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = { /* Yönlendirme */ },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).semantics { contentDescription = "Engellenen Kullanıcılar sayfasını aç" }
        ) {
            Text("Engellenen Kullanıcılar")
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // D. Sohbet ve Bildirimler
        SettingsSectionHeader("D. Sohbet ve Bildirimler")
        SettingsSwitchRow(
            title = "Sesli Mesajları Otomatik Oynat",
            checked = autoPlayVoiceMessages,
            onCheckedChange = { autoPlayVoiceMessages = it },
            desc = "Sesli mesaj otomatik oynatmayı"
        )
        SettingsSwitchRow(
            title = "Odaya Giriş/Çıkış Sesleri",
            checked = roomEntrySounds,
            onCheckedChange = { roomEntrySounds = it },
            desc = "Oda giriş çıkış seslerini"
        )
        SettingsSwitchRow(
            title = "Bahsedilme (Mention) Bildirimleri",
            checked = mentionNotifications,
            onCheckedChange = { mentionNotifications = it },
            desc = "Bahsedilme bildirimlerini"
        )
        SettingsSwitchRow(
            title = "Veri Tasarruf Modu",
            checked = dataSaverMode,
            onCheckedChange = { dataSaverMode = it },
            desc = "Veri tasarruf modunu"
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 16.dp).semantics { heading() }
    )
}

@Composable
fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = "$desc ${if (checked) "kapat" else "aç"}" }
        )
    }
}
