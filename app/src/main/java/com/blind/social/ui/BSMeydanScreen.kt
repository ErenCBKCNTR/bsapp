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
import com.blind.social.data.Gonderi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BSMeydanScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    currentUserId: String
) {
    // Mock Data for the feed
    val mockPosts = remember {
        listOf(
            Gonderi("1", "user1", "Ahmet Yılmaz", "Bugün hava çok güzel, biraz yürüyüş yaptım. Herkese iyi günler dilerim!", "3 saat önce", 15, 4, true),
            Gonderi("2", currentUserId, "Senin Adın", "Yeni özellik olan BS Meydan'ı deniyorum. Erişilebilirlik harika olmuş.", "1 saat önce", 42, 12, false),
            Gonderi("3", "user3", "Ayşe Kaya", "Günün motivasyon sözü: Asla pes etme!", "10 dakika önce", 5, 0, false)
        )
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
                onClick = { /* Yeni Gönderi Yazma Ekranı / Dialog */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics { contentDescription = "Yeni Gönderi Yaz" }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(36.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mockPosts) { post ->
                PostCard(
                    post = post,
                    isOwnPost = post.yazarId == currentUserId,
                    onNavigateToProfile = { onNavigateToProfile(post.yazarId) },
                    onNavigateToPostDetail = { onNavigateToPostDetail(post.id) },
                    onLikeToggle = { /* Beğeni servisine istek */ },
                    onDeletePost = { /* Silme servisine istek */ },
                    onEditPost = { /* Düzenleme ekranına geçiş */ }
                )
            }
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
