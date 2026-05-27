package com.android.favorie.network.model

// POST /items/movies 요청
data class MovieItemRequest(
    val tmdbId: Int,
    val userText: String?,
    val imageUrl: String?
)

// POST /items/books 요청
data class BookItemRequest(
    val bookExternalId: String,
    val userText: String?,
    val imageUrl: String?
)

// POST /items/fashions 요청
data class FashionItemRequest(
    val title: String,
    val userText: String?,
    val imageUrl: String?
)

// POST /items/moods 요청
data class MoodItemRequest(
    val title: String,
    val userText: String?,
    val imageUrl: String?
)

// POST /items/places 요청
// kakaoPlaceId: 카카오맵 연동 전까지는 null 허용
data class PlaceItemRequest(
    val kakaoPlaceId: String?,
    val placeName: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val userText: String?,
    val imageUrl: String?
)

// POST /items/music 요청
data class MusicItemRequest(
    @com.google.gson.annotations.SerializedName("spotifyTrackId") val spotifyId: String?,
    val title: String,
    val userText: String?,
    val imageUrl: String?
)