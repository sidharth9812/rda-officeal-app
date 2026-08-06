package com.example.util

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object LocalCacheManager {
    private const val PREFS_NAME = "rda_local_repository_cache"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun <T> saveList(context: Context?, key: String, list: List<T>, clazz: Class<T>) {
        if (context == null) return
        try {
            val type = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(type)
            val json = adapter.toJson(list)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error saving list for key $key: ${e.message}")
        }
    }

    fun <T> loadList(context: Context?, key: String, clazz: Class<T>): List<T>? {
        if (context == null) return null
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(key, null) ?: return null
            if (json.isBlank()) return null
            val type = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(type)
            adapter.fromJson(json)
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error loading list for key $key: ${e.message}")
            null
        }
    }

    fun <T> saveObject(context: Context?, key: String, obj: T, clazz: Class<T>) {
        if (context == null) return
        try {
            val adapter = moshi.adapter(clazz)
            val json = adapter.toJson(obj)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error saving object for key $key: ${e.message}")
        }
    }

    fun <T> loadObject(context: Context?, key: String, clazz: Class<T>): T? {
        if (context == null) return null
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(key, null) ?: return null
            if (json.isBlank()) return null
            val adapter = moshi.adapter(clazz)
            adapter.fromJson(json)
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error loading object for key $key: ${e.message}")
            null
        }
    }
}
