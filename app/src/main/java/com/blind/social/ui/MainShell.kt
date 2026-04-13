package com.blind.social.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.blind.social.data.KimlikDeposu
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    onLogoutSuccess: () -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    isDesign2: Boolean,
    onToggleDesign2: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val kimlikDeposu = remember { KimlikDeposu() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menü", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text(if (isDarkMode) "Aydınlık Tema Geç" else "Karanlık Tema Geç") },
                    selected = false,
                    onClick = {
                        onToggleTheme(!isDarkMode)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Tema Değiştir") },
                    modifier = Modifier.semantics { contentDescription = "Tema değiştirmek için çift dokunun" }
                )

                NavigationDrawerItem(
                    label = { Text(if (isDesign2) "Orijinal Tasarıma Dön" else "Tasarım 2'ye Geç") },
                    selected = false,
                    onClick = {
                        onToggleDesign2(!isDesign2)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Style, contentDescription = "Tasarım Değiştir") },
                    modifier = Modifier.semantics { contentDescription = "Uygulama tasarımını değiştirmek için çift dokunun" }
                )

                NavigationDrawerItem(
                    label = { Text("Gizlilik Ayarları") },
                    selected = currentRoute == "privacy",
                    onClick = {
                        navController.navigate("privacy")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Gizlilik Ayarları") },
                    modifier = Modifier.semantics { contentDescription = "Gizlilik ayarları için çift dokunun" }
                )

                NavigationDrawerItem(
                    label = { Text("Uygulama Hakkında") },
                    selected = currentRoute == "about",
                    onClick = {
                        navController.navigate("about")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Hakkında") },
                    modifier = Modifier.semantics { contentDescription = "Uygulama hakkında için çift dokunun" }
                )

                NavigationDrawerItem(
                    label = { Text("Çıkış Yap") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            val result = kimlikDeposu.oturumKapat()
                            if (result.isSuccess) {
                                onLogoutSuccess()
                            }
                            drawerState.close()
                        }
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Çıkış Yap") },
                    modifier = Modifier.semantics { contentDescription = "Oturumu kapatmak için çift dokunun" }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Blind Social") },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.semantics { contentDescription = "Yan menüyü açmak için çift dokunun" }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menüyü Aç")
                        }
                    }
                )
            },
            bottomBar = {
                val items = listOf(
                    BottomNavItem("home", "Ana Sayfa", Icons.Default.Home, "Ana sayfaya gitmek için çift dokunun"),
                    BottomNavItem("rooms", "Odalar", Icons.Default.MeetingRoom, "Odalara gitmek için çift dokunun"),
                    BottomNavItem("messages", "Mesajlar", Icons.Default.Message, "Mesajlara gitmek için çift dokunun"),
                    BottomNavItem("profile", "Profil", Icons.Default.Person, "Profile gitmek için çift dokunun")
                )
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = item.contentDesc }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") { HomeScreen() }
                composable("rooms") {
                    RoomsScreen(onNavigateToChat = { roomId, roomName, creatorId ->
                        val safeCreatorId = creatorId?.toString() ?: "null"
                        navController.navigate("chat/$roomId/$roomName?creatorId=$safeCreatorId")
                    })
                }
                composable("messages") { MessagesScreen() }
                composable("profile") { ProfileScreen() }
                composable("privacy") { PrivacySettingsScreen() }
                composable("about") { AboutScreen() }

                composable(
                    route = "chat/{roomId}/{roomName}?creatorId={creatorId}",
                    arguments = listOf(
                        navArgument("roomId") { type = NavType.StringType },
                        navArgument("roomName") { type = NavType.StringType },
                        navArgument("creatorId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = "null"
                        }
                    )
                ) { backStackEntry ->
                    val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                    val encodedName = backStackEntry.arguments?.getString("roomName") ?: ""
                    val roomName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                    val rawCreatorId = backStackEntry.arguments?.getString("creatorId")
                    val creatorId = if (rawCreatorId == "null" || rawCreatorId == null) null else rawCreatorId

                    ChatScreen(
                        roomId = roomId,
                        roomName = roomName,
                        creatorId = creatorId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val contentDesc: String)