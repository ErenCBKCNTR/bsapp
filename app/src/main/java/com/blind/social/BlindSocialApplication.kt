package com.blind.social

import android.app.Application
import android.content.Context
import android.util.Log

class BlindSocialApplication : Application() {
    override fun onCreate() {
        super.onCreate()

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