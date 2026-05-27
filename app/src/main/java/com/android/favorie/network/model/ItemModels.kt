package com.android.favorie.network.model

// GET /items, GET /items/{itemId}, POST /items/* 공통 응답
data class ItemResponse(
    val id: Long,
    val category: String,   // API 카테고리: MOVIE, BOOK, MUSIC, PLACE, FASHION, MOOD
    val title: String,
    val userText: String?,
    val imageUrl: String?,
    val globalTags: List<String>?,
    val localTags: List<String>?,
    val createdAt: String?,
    val vectorStatus: String? = null,   // PENDING, PROCESSING, COMPLETED, FAILED
    val meta: Map<String, Any?>? = null  // MOVIE/BOOK/MUSIC/PLACE만 존재, FASHION/MOOD는 null
)

// GET /items 응답 (커서 기반 무한스크롤)
data class ItemListResponse(
    val items: List<ItemResponse>,
    val hasNext: Boolean,
    val nextCursor: Long?
)

// GET /items/{itemId}/status 응답
data class ItemStatusResponse(
    val id: Long,
    val vectorStatus: String   // PENDING, PROCESSING, COMPLETED, FAILED
)

// GET /s3/presigned-url 응답
data class PresignedUrlResponse(
    val uploadUrl: String,
    val objectUrl: String
)