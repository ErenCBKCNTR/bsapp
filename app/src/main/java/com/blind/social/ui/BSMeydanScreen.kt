package com.blind.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blind.social.data.BSMeydanDeposu
import com.blind.social.data.Gonderi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BSMeydanScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    currentUserId: String
) {
    val meydanDeposu = remember { BSMeydanDeposu() }
    val coroutineScope = rememberCoroutineScope()
    var posts by remember { mutableStateOf<List<Gonderi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        meydanDeposu.gonderileriGercekZamanliDinle().collect { result ->
            if (result.isSuccess) {
                posts = result.getOrDefault(emptyList())
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BS Meydan",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.semantics { contentDescription = "Yan menüyü aç" }
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics { contentDescription = "Yeni Gönderi Yaz" }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(36.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading && posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Henüz bir gönderi yok. İlk gönderiyi sen paylaş!",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { contentDescription = "Şu anda aktif bir gönderi bulunamadı. İlk gönderiyi paylaşmak için sağ alt köşedeki Yeni Gönderi Yaz butonuna dokunun." }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(posts) { post ->
                    PostCard(
                        post = post,
                        isOwnPost = post.yazarId == currentUserId,
                        onNavigateToProfile = { onNavigateToProfile(post.yazarId) },
                        onNavigateToPostDetail = { onNavigateToPostDetail(post.id) },
                        onLikeToggle = {
                            coroutineScope.launch {
                                meydanDeposu.gonderiBegenVeyaGeriAl(post.id, post.isLikedByMe)
                            }
                        },
                        onDeletePost = {
                            coroutineScope.launch { meydanDeposu.gonderiSil(post.id) }
                        },
                        onEditPost = { /* Düzenleme ekranı daha sonra eklenebilir */ }
                    )
                }
            }
        }

        if (showCreateDialog) {
            var icerik by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Yeni Gönderi") },
                text = {
                    OutlinedTextField(
                        value = icerik,
                        onValueChange = { if (it.length <= 500) icerik = it },
                        label = { Text("Düşüncelerin...") },
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                            .semantics { contentDescription = "Gönderi metnini yazın. En fazla 500 karakter." }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (icerik.isNotBlank()) {
                            coroutineScope.launch {
                                meydanDeposu.gonderiOlustur(icerik)
                                showCreateDialog = false
                            }
                        }
                    }) {
                        Text("Paylaş")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}

@Composable
fun PostCard(
    post: Gonderi,
    isOwnPost: Boolean,
    onNavigateToProfile: () -> Unit,
    onNavigateToPostDetail: () -> Unit,
    onLikeToggle: () -> Unit,
    onDeletePost: () -> Unit,
    onEditPost: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Ana Odak Bloğu (TalkBack için tüm içerik burada birleştirilir)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProfile() }
                    .padding(16.dp)
                    .clearAndSetSemantics {
                        contentDescription = "${post.yazarAdiSoyadi}. ${post.icerik}. ${post.zamanDilimi}. ${post.begeniSayisi} beğeni, ${post.yorumSayisi} yorum."
                        onClick(label = "Profili Görüntüle") {
                            onNavigateToProfile()
                            true
                        }
                        if (isOwnPost) {
                            customActions = listOf(
                                CustomAccessibilityAction("Sil") {
                                    onDeletePost()
                                    true
                                },
                                CustomAccessibilityAction("Düzenle") {
                                    onEditPost()
                                    true
                                }
                            )
                        }
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = post.yazarAdiSoyadi,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                        Text(
                            text = post.zamanDilimi,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.icerik,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Etkileşim Butonları (TalkBack için ayrı odaklanabilir bölge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onLikeToggle,
                    modifier = Modifier.semantics {
                        contentDescription = if (post.isLikedByMe) "Beğenmekten Vazgeç" else "Beğen"
                    }
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (post.isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${post.begeniSayisi}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clearAndSetSemantics { })
                }

                Spacer(modifier = Modifier.width(16.dp))

                TextButton(
                    onClick = onNavigateToPostDetail,
                    modifier = Modifier.semantics {
                        contentDescription = "Yorum yap"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${post.yorumSayisi}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clearAndSetSemantics { })
                }
            }
        }
    }
}
