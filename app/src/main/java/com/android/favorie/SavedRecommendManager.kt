package com.android.favorie

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SavedRecommendManager {
    private const val PREFS_NAME = "saved_recommends"
    private const val KEY_ITEMS = "items"
    private val gson = Gson()

    fun saveItem(context: Context, item: RecommendItem) {
        val current = getItems(context).toMutableList()
        if (current.none { it.itemId == item.itemId }) {
            current.add(0, item)
            persist(context, current)
        }
    }

    fun getItems(context: Context): List<RecommendItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        val type = object : TypeToken<List<RecommendItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(context: Context, items: List<RecommendItem>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, gson.toJson(items))
            .apply()
    }
}
