package com.android.favorie

import com.android.favorie.network.model.RecommendItemMeta

data class RecommendItem(
    val categoryName: String,
    val categoryColor: String,
    val moodText: String,
    val imageRes: Int,
    val itemTitle: String,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val itemId: Long = 0L,
    val recommendationId: Long = 0L,
    val apiCategory: String = "",
    val meta: RecommendItemMeta? = null
)
