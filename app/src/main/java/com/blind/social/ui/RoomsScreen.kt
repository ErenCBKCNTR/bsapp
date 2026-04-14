package com.blind.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.blind.social.data.Oda
import com.blind.social.data.OdaDeposu
import com.blind.social.prefs.ThemePreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(onNavigateToChat: (String, String, String?) -> Unit) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isDesign2 by themePreferences.isDesign2.collectAsState(initial = true)

    val odaDeposu = remember { OdaDeposu() }
    val coroutineScope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var rooms by remember { mutableStateOf<List<Oda>>(emptyList()) }

    // Password dialog state
    var pendingRoom by remember { mutableStateOf<Oda?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tümü") }
    var showOnlyUnprotected by remember { mutableStateOf(false) }
    var expandedCategoryFilter by remember { mutableStateOf(false) }
    val filterCategories = listOf("Tümü", "Genel", "Siyaset", "Teknoloji", "Oyun", "Müzik", "Eğitim", "Edebiyat")

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        odaDeposu.odalariGercekZamanliDinle().collect { result ->
            if (result.isSuccess) {
                rooms = result.getOrDefault(emptyList())
            }
            isLoading = false
        }
    }

    val filteredRooms = rooms.filter {
        val matchesSearch = it.odaAdi.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "Tümü" || it.kategori == selectedCategory
        val matchesProtection = if (showOnlyUnprotected) it.sifre.isNullOrBlank() else true
        matchesSearch && matchesCategory && matchesProtection
    }

    Scaffold(
        topBar = {
            if (isDesign2) {
                TopAppBar(
                    title = {
                        Text(
                            "BLIND SOCIAL",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() }
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showFilters = !showFilters },
                            modifier = Modifier.semantics { contentDescription = "Oda filtrelerini ${if (showFilters) "gizle" else "göster"}" }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        floatingActionButton = {
            if (isDesign2) {
                LargeFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.semantics { contentDescription = "Yeni Oda Oluştur" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(36.dp))
                }
            } else {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.semantics { contentDescription = "Yeni oda oluşturmak için çift dokunun" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Oluştur")
                }
            }
        },
        containerColor = if (isDesign2) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (isDesign2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Aktif Odalar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier.semantics { contentDescription = "Oda filtrelerini ${if (showFilters) "gizle" else "göster"}" }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                    }
                }
            }

            // Filter Section with AnimatedVisibility
            androidx.compose.animation.AnimatedVisibility(visible = showFilters) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Oda Ara") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        colors = if (isDesign2) TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface) else TextFieldDefaults.colors()
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
                                colors = if (isDesign2) ExposedDropdownMenuDefaults.textFieldColors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface) else ExposedDropdownMenuDefaults.textFieldColors(),
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
                            Text(text = "Sadece Şifresiz", color = if (isDesign2) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = showOnlyUnprotected,
                                onCheckedChange = { showOnlyUnprotected = it },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // Room List
            if (isLoading && rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Şu anda aktif bir oda bulunamadı.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { contentDescription = "Şu anda aktif bir oda bulunamadı." }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = if (isDesign2) Arrangement.spacedBy(16.dp) else Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(if (isDesign2) rooms else filteredRooms) { room ->
                        if (isDesign2) {
                            Design2RoomCard(room = room, onClick = {
                                handleRoomClick(
                                    room,
                                    { pendingRoom = it; passwordInput = ""; passwordError = false },
                                    { rId, rName, cId -> onNavigateToChat(rId, rName, cId) }
                                )
                            })
                        } else {
                            OriginalRoomCard(room = room, onClick = {
                                handleRoomClick(
                                    room,
                                    { pendingRoom = it; passwordInput = ""; passwordError = false },
                                    { rId, rName, cId -> onNavigateToChat(rId, rName, cId) }
                                )
                            })
                        }
                    }
                }
            }
        }

        // Password Dialog
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
                            val createdRoom = result.getOrNull()
                            createdRoom?.id?.let { roomId ->
                                val encodedName = java.net.URLEncoder.encode(createdRoom.odaAdi, "UTF-8")
                                onNavigateToChat(roomId, encodedName, createdRoom.kurucuId)
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun handleRoomClick(
    room: Oda,
    onRequirePassword: (Oda) -> Unit,
    onNavigate: (String, String, String?) -> Unit
) {
    room.id?.let { roomId ->
        if (!room.sifre.isNullOrBlank()) {
            onRequirePassword(room)
        } else {
            val encodedName = java.net.URLEncoder.encode(room.odaAdi, "UTF-8")
            onNavigate(roomId, encodedName, room.kurucuId)
        }
    }
}

@Composable
fun OriginalRoomCard(room: Oda, onClick: () -> Unit) {
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
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Odaya Bağlan")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Design2RoomCard(room: Oda, onClick: () -> Unit) {
    val isProtected = !room.sifre.isNullOrBlank()

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${room.odaAdi} Odası, ${room.kategori} Kategorisi, ${room.kapasite} Kişilik, ${if (isProtected) "Şifreli" else "Şifresiz"}. Odaya katılmak için çift tıklayın."
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isProtected) Icons.Default.Lock else Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.odaAdi,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = room.kategori,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${room.kapasite} Kişi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
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
