package com.example.spendwise.util

import android.content.Context
import android.content.SharedPreferences

object AiConfigManager {
    private const val PREFS_NAME = "spendwise_ai_config_prefs"
    private const val KEY_CLOUD_AI_ENABLED = "key_cloud_ai_enabled"
    private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
    private const val KEY_OFFLINE_FALLBACK = "key_offline_fallback"

    // Default built-in key or user-configured key
    // Users can easily get their own free key from https://aistudio.google.com/
    private const val DEFAULT_API_KEY = ""

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isCloudAiEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CLOUD_AI_ENABLED, true)
    }

    fun setCloudAiEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CLOUD_AI_ENABLED, enabled).apply()
    }

    fun getGeminiApiKey(context: Context): String {
        val key = getPrefs(context).getString(KEY_GEMINI_API_KEY, null)
        return if (!key.isNullOrBlank()) key else DEFAULT_API_KEY
    }

    fun setGeminiApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun isOfflineFallbackEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_OFFLINE_FALLBACK, true)
    }

    fun setOfflineFallbackEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_OFFLINE_FALLBACK, enabled).apply()
    }
}
