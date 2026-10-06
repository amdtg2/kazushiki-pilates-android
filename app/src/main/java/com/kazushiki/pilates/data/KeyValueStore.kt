package com.kazushiki.pilates.data

import android.content.Context
import android.content.SharedPreferences

/** Small key-value storage (UserDefaults on iPhone). Swappable for an in-memory one in tests. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getBoolean(key: String): Boolean?
    fun putBoolean(key: String, value: Boolean)
}

class SharedPrefsStore(context: Context, name: String = "kazushiki_pilates") : KeyValueStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
    override fun getBoolean(key: String): Boolean? = if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}

class MemoryStore : KeyValueStore {
    private val values = mutableMapOf<String, Any>()
    override fun getString(key: String) = values[key] as? String
    override fun putString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
    override fun getBoolean(key: String) = values[key] as? Boolean
    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}

/** Lenient JSON so older saved data (missing fields) still loads. */
val AppJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
