package com.linkrunner.sdk.utils

import android.content.Context
import android.content.SharedPreferences
import com.linkrunner.sdk.BuildConfig

/**
 * Helper class to manage SharedPreferences operations
 */
internal class PreferenceManager(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("${BuildConfig.LIBRARY_PACKAGE_NAME}_prefs", Context.MODE_PRIVATE)
    }

    private val editor: SharedPreferences.Editor by lazy {
        prefs.edit()
    }

    fun saveString(key: String, value: String) {
        editor.putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun saveBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun remove(key: String) {
        editor.remove(key).apply()
    }

    fun clear() {
        editor.clear().apply()
    }
}
