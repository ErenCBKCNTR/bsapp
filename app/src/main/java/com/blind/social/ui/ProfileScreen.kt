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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "."
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return 10
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isDesign2 by themePreferences.isDesign2.collectAsState(initial = true)

    val profilDeposu = remember { ProfilDeposu() }
    val kimlikDeposu = remember { com.blind.social.data.KimlikDeposu() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Profil form verileri
    var ad by remember { mutableStateOf("") }
    var soyad by remember { mutableStateOf("") }
    var hakkimda by remember { mutableStateOf("") }
    var isPasswordEditing by remember { mutableStateOf(false) }
    var yeniSifre by remember { mutableStateOf("") }
    var yeniSifreTekrar by remember { mutableStateOf("") }
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
                ad = profil.ad ?: ""
                soyad = profil.soyad ?: ""

                hakkimda = profil.hakkimda ?: ""
                kullaniciAdi = profil.kullaniciAdi
                eposta = profil.email
                baglantilar = profil.baglantilar ?: ""
                dogumTarihi = profil.dogumTarihi
            }
        } else {
            // Hata durumunda bile fallback bir kere daha denenmiş olur ancak ekranda hata göstermeden formu boş bırakabiliriz
            snackbarHostState.showSnackbar("Profil yüklenirken hata oluştu, lütfen sayfayı yenileyin.")
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
                        onValueChange = { if (it.length <= 50) ad = it },
                        label = { Text("Ad") },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Adınızı girin, en fazla 50 karakter" }
                    )
                    OutlinedTextField(
                        value = soyad,
                        onValueChange = { if (it.length <= 50) soyad = it },
                        label = { Text("Soyad") },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Soyadınızı girin, en fazla 50 karakter" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Diğer Alt Alta Alanlar
                OutlinedTextField(
                    value = hakkimda,
                    onValueChange = { if (it.length <= 150) hakkimda = it },
                    label = { Text("Hakkımda") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Hakkımda alanını girin, en fazla 150 karakter" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = kullaniciAdi,
                    onValueChange = { if (it.length <= 30) kullaniciAdi = it },
                    label = { Text("Kullanıcı Adı") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Kullanıcı Adı alanını girin, en fazla 30 karakter" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = eposta,
                    onValueChange = { if (it.length <= 254) eposta = it },
                    label = { Text("E-posta") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "E-posta alanını girin, en fazla 254 karakter" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = baglantilar,
                    onValueChange = { if (it.length <= 200) baglantilar = it },
                    label = { Text("Bağlantılar") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Bağlantılar alanını girin, en fazla 200 karakter" }
                )
                Spacer(modifier = Modifier.height(16.dp))

                val dogumTarihiFormated = if (dogumTarihi.length == 8) {
                    "${dogumTarihi.substring(0, 2)}.${dogumTarihi.substring(2, 4)}.${dogumTarihi.substring(4, 8)}"
                } else dogumTarihi

                OutlinedTextField(
                    value = dogumTarihi,
                    onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) dogumTarihi = it },
                    label = { Text("Doğum Tarihi") },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Doğum Tarihi. Şu anki değer: $dogumTarihiFormated"
                    },
                    visualTransformation = DateVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(8.dp))

                // Şifreyi Değiştir Butonu ve Açılır Kapanır Alanı
                TextButton(
                    onClick = { isPasswordEditing = !isPasswordEditing },
                    modifier = Modifier.semantics { contentDescription = "Şifreyi Değiştir alanını ${if (isPasswordEditing) "kapat" else "aç"}" }
                ) {
                    Text(
                        text = "Şifreyi Değiştir",
                        color = if (isDesign2) MaterialTheme.colorScheme.onBackground else Color.DarkGray
                    )
                }

                androidx.compose.animation.AnimatedVisibility(visible = isPasswordEditing) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = yeniSifre,
                            onValueChange = { if (it.length <= 64) yeniSifre = it },
                            label = { Text("Yeni Şifre") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Yeni Şifrenizi girin (en az 6, en fazla 64 karakter)" },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = yeniSifreTekrar,
                            onValueChange = { if (it.length <= 64) yeniSifreTekrar = it },
                            label = { Text("Yeni Şifre (Tekrar)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Yeni Şifrenizi tekrar girin" },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profili Güncelle Butonu
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updatedAd = ad.trim()
                            val updatedSoyad = soyad.trim()

                            // Check if any fields actually changed
                            val hasChanges = mevcutProfil == null ||
                                mevcutProfil?.ad != updatedAd ||
                                mevcutProfil?.soyad != updatedSoyad ||
                                mevcutProfil?.kullaniciAdi != kullaniciAdi ||
                                mevcutProfil?.email != eposta ||
                                mevcutProfil?.hakkimda != hakkimda ||
                                mevcutProfil?.baglantilar != baglantilar ||
                                mevcutProfil?.dogumTarihi != dogumTarihi ||
                                (isPasswordEditing && (yeniSifre.isNotBlank() || yeniSifreTekrar.isNotBlank()))

                            if (!hasChanges) {
                                snackbarHostState.showSnackbar("Herhangi bir değişiklik yapılmadı.")
                                return@launch
                            }

                            if (isPasswordEditing && yeniSifre != yeniSifreTekrar) {
                                snackbarHostState.showSnackbar("Şifreler eşleşmiyor.")
                                return@launch
                            }

                            isSaving = true

                            val profilToSave = mevcutProfil?.copy(
                                ad = updatedAd,
                                soyad = updatedSoyad,
                                kullaniciAdi = kullaniciAdi,
                                email = eposta, // Note: Supabase auth email requires auth.updateUser, this only updates the public profile email
                                hakkimda = hakkimda,
                                baglantilar = baglantilar,
                                dogumTarihi = dogumTarihi
                            ) ?: KullaniciProfili(
                                email = eposta,
                                kullaniciAdi = kullaniciAdi,
                                ad = updatedAd,
                                soyad = updatedSoyad,
                                dogumTarihi = dogumTarihi,
                                hakkimda = hakkimda,
                                baglantilar = baglantilar
                            )

                            val result = profilDeposu.profilGuncelle(profilToSave)
                            if (result.isSuccess) {
                                mevcutProfil = profilToSave

                                if (isPasswordEditing && yeniSifre.isNotBlank()) {
                                    val pwResult = kimlikDeposu.sifreGuncelle(yeniSifre)
                                    if (pwResult.isSuccess) {
                                        yeniSifre = ""
                                        yeniSifreTekrar = ""
                                        isPasswordEditing = false
                                        snackbarHostState.showSnackbar("Profil ve şifre başarıyla güncellendi.")
                                    } else {
                                        snackbarHostState.showSnackbar("Profil güncellendi ancak ŞİFRE DEĞİŞTİRİLEMEDİ: ${pwResult.exceptionOrNull()?.message}")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Profil başarıyla güncellendi.")
                                }
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
