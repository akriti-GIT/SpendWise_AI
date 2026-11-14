package com.example.spendwise.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("spendwise_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_USER_ID = "key_active_user_id"
    }

    fun getActiveUserId(): String? {
        val id = prefs.getString(KEY_ACTIVE_USER_ID, null)
        return if (id.isNullOrBlank()) null else id
    }

    fun setActiveUserId(userId: String?) {
        prefs.edit().apply {
            if (!userId.isNullOrBlank()) {
                putString(KEY_ACTIVE_USER_ID, userId)
            } else {
                remove(KEY_ACTIVE_USER_ID)
            }
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
