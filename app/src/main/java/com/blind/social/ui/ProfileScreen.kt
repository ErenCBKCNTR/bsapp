package com.blind.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blind.social.data.KullaniciProfili
import com.blind.social.data.ProfilDeposu
import com.blind.social.prefs.ThemePreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isDesign2 by themePreferences.isDesign2.collectAsState(initial = false)

    val profilDeposu = remember { ProfilDeposu() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Profil form verileri
    var ad by remember { mutableStateOf("") }
    var soyad by remember { mutableStateOf("") }
    var hakkimda by remember { mutableStateOf("") }
    var kullaniciAdi by remember { mutableStateOf("") }
    var eposta by remember { mutableStateOf("") }
    var baglantilar by remember { mutableStateOf("") }
    var dogumTarihi by remember { mutableStateOf("") }
    var mevcutProfil by remember { mutableStateOf<KullaniciProfili?>(null) }

    LaunchedEffect(Unit) {
        val result = profilDeposu.profilGetir()
        if (result.isSuccess) {
            val profil = result.getOrNull()
            mevcutProfil = profil
            if (profil != null) {
                // Ad Soyad parse etme
                val isimParcalari = profil.adSoyad.split(" ", limit = 2)
                ad = isimParcalari.firstOrNull() ?: ""
                soyad = if (isimParcalari.size > 1) isimParcalari[1] else ""

                hakkimda = profil.hakkimda ?: ""
                kullaniciAdi = profil.kullaniciAdi
                eposta = profil.email
                baglantilar = profil.baglantilar ?: ""
                dogumTarihi = profil.dogumTarihi
            }
        } else {
            snackbarHostState.showSnackbar("Profil yüklenirken hata oluştu.")
        }
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = if (isDesign2) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Üst Başlık (Eğer Scaffold dışındaysa)
                Text(
                    text = "Profil",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDesign2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp)
                )

                // Standart Gri Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(if (isDesign2) MaterialTheme.colorScheme.surface else Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profil Fotoğrafı",
                        modifier = Modifier.size(80.dp),
                        tint = if (isDesign2) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Ad ve Soyad Yan Yana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ad,
                        onValueChange = { ad = it },
                        label = { Text("Ad") },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Adınızı girin" }
                    )
                    OutlinedTextField(
                        value = soyad,
                        onValueChange = { soyad = it },
                        label = { Text("Soyad") },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Soyadınızı girin" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Diğer Alt Alta Alanlar
                OutlinedTextField(
                    value = hakkimda,
                    onValueChange = { hakkimda = it },
                    label = { Text("Hakkımda") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Hakkımda alanını girin" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = kullaniciAdi,
                    onValueChange = { kullaniciAdi = it },
                    label = { Text("Kullanıcı Adı") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Kullanıcı Adı alanını girin" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = eposta,
                    onValueChange = { eposta = it },
                    label = { Text("E-posta") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "E-posta alanını girin" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = baglantilar,
                    onValueChange = { baglantilar = it },
                    label = { Text("Bağlantılar") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Bağlantılar alanını girin" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dogumTarihi,
                    onValueChange = { dogumTarihi = it },
                    label = { Text("Doğum Tarihi (GG.AA.YYYY)") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Doğum Tarihi (GG.AA.YYYY) alanını girin" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(8.dp))

                // Şifreyi Değiştir Butonu
                TextButton(
                    onClick = { /* Şifre değiştirme işlemi */ },
                    modifier = Modifier.semantics { contentDescription = "Şifreyi Değiştir ekranını aç" }
                ) {
                    Text(
                        text = "Şifreyi Değiştir",
                        color = if (isDesign2) MaterialTheme.colorScheme.onBackground else Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profili Güncelle Butonu
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSaving = true
                            val updatedAdSoyad = listOf(ad.trim(), soyad.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                            val profilToSave = mevcutProfil?.copy(
                                adSoyad = updatedAdSoyad,
                                kullaniciAdi = kullaniciAdi,
                                email = eposta, // Note: Supabase auth email requires auth.updateUser, this only updates the public profile email
                                hakkimda = hakkimda,
                                baglantilar = baglantilar,
                                dogumTarihi = dogumTarihi
                            ) ?: KullaniciProfili(
                                email = eposta,
                                kullaniciAdi = kullaniciAdi,
                                adSoyad = updatedAdSoyad,
                                dogumTarihi = dogumTarihi,
                                hakkimda = hakkimda,
                                baglantilar = baglantilar
                            )

                            val result = profilDeposu.profilGuncelle(profilToSave)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("Profil başarıyla güncellendi.")
                            } else {
                                snackbarHostState.showSnackbar("Profil güncellenemedi: ${result.exceptionOrNull()?.message}")
                            }
                            isSaving = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "Profil değişikliklerini kaydet" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDesign2) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                    ),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Profili Güncelle",
                            color = if (isDesign2) MaterialTheme.colorScheme.onPrimary else Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
