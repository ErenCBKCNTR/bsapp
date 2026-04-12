package com.blind.social.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.blind.social.data.Oda
import com.blind.social.data.OdaDeposu
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(onNavigateToChat: (String, String, String?) -> Unit) {
    val odaDeposu = remember { OdaDeposu() }
    val coroutineScope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var rooms by remember { mutableStateOf<List<Oda>>(emptyList()) }

    // Password dialog state
    var pendingRoom by remember { mutableStateOf<Oda?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    // Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tümü") }
    var showOnlyUnprotected by remember { mutableStateOf(false) }
    var expandedCategoryFilter by remember { mutableStateOf(false) }
    val filterCategories = listOf("Tümü", "Genel", "Siyaset", "Teknoloji", "Oyun", "Müzik", "Eğitim", "Edebiyat")

    LaunchedEffect(Unit) {
        odaDeposu.odalariGercekZamanliDinle().collect { result ->
            if (result.isSuccess) {
                rooms = result.getOrDefault(emptyList())
            }
        }
    }

    val filteredRooms = rooms.filter {
        val matchesSearch = it.odaAdi.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "Tümü" || it.kategori == selectedCategory
        val matchesProtection = if (showOnlyUnprotected) it.sifre.isNullOrBlank() else true
        matchesSearch && matchesCategory && matchesProtection
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.semantics { contentDescription = "Yeni oda oluşturmak için çift dokunun" }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Oluştur")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Filter Section
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Oda Ara") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    ExposedDropdownMenuBox(
                        expanded = expandedCategoryFilter,
                        onExpandedChange = { expandedCategoryFilter = !expandedCategoryFilter },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        TextField(
                            readOnly = true,
                            value = selectedCategory,
                            onValueChange = {},
                            label = { Text("Kategori") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryFilter) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoryFilter,
                            onDismissRequest = { expandedCategoryFilter = false }
                        ) {
                            filterCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCategoryFilter = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sadece Şifresiz")
                        Switch(
                            checked = showOnlyUnprotected,
                            onCheckedChange = { showOnlyUnprotected = it },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Room List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRooms) { room ->
                    val isProtected = !room.sifre.isNullOrBlank()
                    val sifreDurumuText = if (isProtected) "Şifreli" else "Şifresiz"
                    val a11yDesc = "${room.odaAdi}, ${room.kategori}, ${room.kapasite} kişilik, $sifreDurumuText. Bağlanmak için çift dokunun"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = a11yDesc
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = room.odaAdi,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                if (isProtected) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Şifreli Oda",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 4.dp),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Kategori: ${room.kategori} | ${room.kapasite} Kişilik Kapasite",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                            )

                            Button(
                                onClick = {
                                    room.id?.let { roomId ->
                                        if (!room.sifre.isNullOrBlank()) {
                                            // Şifreli oda: önce şifre sor
                                            pendingRoom = room
                                            passwordInput = ""
                                            passwordError = false
                                        } else {
                                            val encodedName = java.net.URLEncoder.encode(room.odaAdi, "UTF-8")
                                            onNavigateToChat(roomId, encodedName, room.kurucuId)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Odaya Bağlan")
                            }
                        }
                    }
                }
            }
        }

        // Şifreli oda giriş dialogu
        if (pendingRoom != null) {
            AlertDialog(
                onDismissRequest = { pendingRoom = null },
                title = { Text("Şifreli Oda") },
                text = {
                    Column {
                        Text("\"${pendingRoom!!.odaAdi}\" odasına girmek için şifre gerekiyor.")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = false
                            },
                            label = { Text("Şifre") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = passwordError,
                            supportingText = if (passwordError) { { Text("Şifre yanlış", color = MaterialTheme.colorScheme.error) } } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val room = pendingRoom!!
                        if (passwordInput == room.sifre) {
                            val encodedName = java.net.URLEncoder.encode(room.odaAdi, "UTF-8")
                            onNavigateToChat(room.id!!, encodedName, room.kurucuId)
                            pendingRoom = null
                        } else {
                            passwordError = true
                        }
                    }) {
                        Text("Gir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRoom = null }) {
                        Text("İptal")
                    }
                }
            )
        }

        if (showCreateDialog) {
            CreateRoomDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { newOda ->
                    coroutineScope.launch {
                        val result = odaDeposu.odaOlustur(newOda)
                        if (result.isSuccess) {
                            showCreateDialog = false
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (Oda) -> Unit
) {
    var odaAdi by remember { mutableStateOf("") }
    var kapasite by remember { mutableStateOf(3) }
    var kategori by remember { mutableStateOf("Genel") }
    var sifre by remember { mutableStateOf("") }

    var expandedCapacity by remember { mutableStateOf(false) }
    val capacities = listOf(3, 7, 14, 21, 30)

    var expandedCategory by remember { mutableStateOf(false) }
    val categories = listOf("Genel", "Siyaset", "Teknoloji", "Oyun", "Müzik", "Eğitim", "Edebiyat")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oda Oluştur") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = odaAdi,
                    onValueChange = { odaAdi = it },
                    label = { Text("Oda Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // Capacity Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCapacity,
                    onExpandedChange = { expandedCapacity = !expandedCapacity },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    TextField(
                        readOnly = true,
                        value = "$kapasite Kişilik",
                        onValueChange = {},
                        label = { Text("Kapasite") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCapacity) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCapacity,
                        onDismissRequest = { expandedCapacity = false }
                    ) {
                        capacities.forEach { cap ->
                            DropdownMenuItem(
                                text = { Text("$cap Kişilik") },
                                onClick = {
                                    kapasite = cap
                                    expandedCapacity = false
                                }
                            )
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    TextField(
                        readOnly = true,
                        value = kategori,
                        onValueChange = {},
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    kategori = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                // Password
                TextField(
                    value = sifre,
                    onValueChange = { newValue ->
                        if (newValue.length <= 10 && newValue.all { it.isLetterOrDigit() }) {
                            sifre = newValue
                        }
                    },
                    label = { Text("Şifre (İsteğe Bağlı, Maks 10)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (odaAdi.isNotBlank()) {
                        onCreate(
                            Oda(
                                odaAdi = odaAdi,
                                kapasite = kapasite,
                                kategori = kategori,
                                sifre = sifre.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}