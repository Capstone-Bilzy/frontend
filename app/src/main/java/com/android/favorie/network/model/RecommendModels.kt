package com.android.favorie.network.model

import com.google.gson.annotations.SerializedName

// GET /recommend/my — metadata 포함 전체 추천 목록
data class MyRecommendResponse(
    @SerializedName("results") val results: List<MyRecommendResult>
)

data class MyRecommendResult(
    @SerializedName("recommendationId") val recommendationId: Long,
    @SerializedName("category") val category: String,
    @SerializedName("itemId") val itemId: Long,
    @SerializedName("meta") val meta: RecommendItemMeta?
)

// GET /recommend/category — metadata 없는 일일 추천 목록
data class RecommendResponse(
    @SerializedName("results") val results: List<RecommendResult>
)

data class RecommendResult(
    @SerializedName("recommendationId") val recommendationId: Long,
    @SerializedName("category") val category: String,
    @SerializedName("itemId") val itemId: Long
)

// GET /recommend/items/{itemId}
data class RecommendItemDetailResponse(
    @SerializedName("itemId") val itemId: Long,
    @SerializedName("category") val category: String,
    @SerializedName("meta") val meta: RecommendItemMeta?
)

data class RecommendItemMeta(
    @SerializedName("externalId") val externalId: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    // MOVIE
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseYear") val releaseYear: Int?,
    // BOOK
    @SerializedName("author") val author: String?,
    // MUSIC
    @SerializedName("artist") val artist: String?,
    @SerializedName("album") val album: String?,
    // PLACE
    @SerializedName("address") val address: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
)
