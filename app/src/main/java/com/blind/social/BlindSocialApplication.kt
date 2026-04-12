package com.blind.social

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BlindSocialApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            try {
                SupabaseModul.client.realtime.connect()
            } catch (e: Exception) {
                Log.e("Realtime", "Failed to connect to Supabase Realtime", e)
            }
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val stackTrace = Log.getStackTraceString(exception)
                val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("last_crash", stackTrace).commit()
            } catch (e: Exception) {
                // Ignore failure during crash handling
            }
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}