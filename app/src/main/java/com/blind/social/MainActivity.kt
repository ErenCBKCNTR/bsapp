package com.blind.social

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.blind.social.ui.HomeScreen
import com.blind.social.ui.LoginScreen
import com.blind.social.ui.RegisterScreen
import com.blind.social.ui.theme.BlindSocialTheme
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        val crashLog = prefs.getString("last_crash", null)

        setContent {
            var showCrashDialog by remember { mutableStateOf(crashLog != null) }

            val context = androidx.compose.ui.platform.LocalContext.current
            val themePreferences = remember { com.blind.social.prefs.ThemePreferences(context) }
            val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
            val isDesign2 by themePreferences.isDesign2.collectAsState(initial = true)

            BlindSocialTheme(darkTheme = isDarkMode, isDesign2 = isDesign2) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showCrashDialog && crashLog != null) {
                        AlertDialog(
                            onDismissRequest = { /* Zorunlu kapanmalı */ },
                            title = { Text("Uygulama Çöktü (Fatal Error)") },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text(crashLog, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        prefs.edit().remove("last_crash").apply()
                                        showCrashDialog = false
                                    }
                                ) {
                                    Text("Temizle ve Kapat")
                                }
                            }
                        )
                    } else {
                        AppNavigation(themePreferences, isDarkMode, isDesign2)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(themePreferences: com.blind.social.prefs.ThemePreferences, isDarkMode: Boolean, isDesign2: Boolean) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        SupabaseModul.client.auth.sessionStatus.collect { status ->
            when (status) {
                is SessionStatus.Authenticated -> startDestination = "home"
                is SessionStatus.NotAuthenticated -> startDestination = "login"
                else -> {}
            }
        }
    }

    if (startDestination == null) {
        return // Yükleniyor...
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            val coroutineScope = rememberCoroutineScope()

            com.blind.social.ui.MainShell(
                onLogoutSuccess = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                isDarkMode = isDarkMode,
                onToggleTheme = { newTheme ->
                    coroutineScope.launch {
                        themePreferences.saveThemePreference(newTheme)
                    }
                },
                isDesign2 = isDesign2,
                onToggleDesign2 = { design2 ->
                    coroutineScope.launch {
                        themePreferences.saveDesignPreference(design2)
                    }
                }
            )
        }
    }
}