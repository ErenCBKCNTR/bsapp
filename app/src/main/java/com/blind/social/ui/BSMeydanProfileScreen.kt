package com.blind.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blind.social.data.BSMeydanDeposu
import com.blind.social.data.Gonderi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BSMeydanProfileScreen(
    userId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit
) {
    val isOwnProfile = userId == currentUserId
    val meydanDeposu = remember { BSMeydanDeposu() }
    val coroutineScope = rememberCoroutineScope()
    var userPosts by remember { mutableStateOf<List<Gonderi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        meydanDeposu.gonderileriGercekZamanliDinle(kullaniciId = userId).collect { result ->
            if (result.isSuccess) {
                userPosts = result.getOrDefault(emptyList())
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BS Meydan Profili") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Geri dön" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Kullanıcı Profil Fotoğrafı",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = userPosts.firstOrNull()?.yazarAdiSoyadi ?: "Kullanıcı",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (isOwnProfile) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* Profili Düzenle Aksiyonu */ },
                        modifier = Modifier.semantics { contentDescription = "Profilini Düzenle" }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profili Düzenle")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Posts List
            Text(
                text = if (isOwnProfile) "Paylaşımlarım" else "Paylaşımlar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
                    .semantics { heading() }
            )

            if (isLoading && userPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (userPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Henüz gönderi yok.",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(userPosts) { post ->
                        PostCard(
                            post = post,
                            isOwnPost = isOwnProfile,
                            onNavigateToProfile = { /* Zaten profil sayfasındayız */ },
                            onNavigateToPostDetail = { onNavigateToPostDetail(post.id) },
                            onLikeToggle = {
                                coroutineScope.launch { meydanDeposu.gonderiBegenVeyaGeriAl(post.id, post.isLikedByMe) }
                            },
                            onDeletePost = {
                                coroutineScope.launch { meydanDeposu.gonderiSil(post.id) }
                            },
                            onEditPost = { }
                        )
                    }
                }
            }
        }
    }
}
