package com.srizwan.task

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * common.js-এর `theme`: সিস্টেম (auto) → লাইট → ডার্ক। `kp_theme_v1` কী-তে সংরক্ষিত
 * (auto হলে কী মুছে ফেলা হয়)। `mode` Compose state, তাই বদলালে পুরো অ্যাপের থিম বদলে যায়।
 */
object ThemeStore {
    private const val PREFS = "kp_theme"
    private const val KEY = "kp_theme_v1"

    var mode by mutableStateOf(ThemeMode.System)
        private set

    fun load(context: Context) {
        val v = try {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        } catch (e: Exception) {
            null
        }
        mode = when (v) {
            "dark" -> ThemeMode.Dark
            "light" -> ThemeMode.Light
            else -> ThemeMode.System
        }
    }

    fun set(context: Context, m: ThemeMode) {
        mode = m
        try {
            val e = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            when (m) {
                ThemeMode.System -> e.remove(KEY)
                ThemeMode.Light -> e.putString(KEY, "light")
                ThemeMode.Dark -> e.putString(KEY, "dark")
            }
            e.apply()
        } catch (ex: Exception) {
            // উপেক্ষা
        }
    }

    fun next(): ThemeMode = when (mode) {
        ThemeMode.System -> ThemeMode.Light
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.System
    }

    fun label(m: ThemeMode = mode): String = when (m) {
        ThemeMode.System -> "সিস্টেম অনুযায়ী"
        ThemeMode.Light -> "লাইট মোড"
        ThemeMode.Dark -> "ডার্ক মোড"
    }
}
