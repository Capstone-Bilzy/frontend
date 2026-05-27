package com.android.favorie
data class MusicItem(
    val id: String,
    val title: String,
    val artist: String,      // 영화면 감독, 책이면 저자, 공간이면 위치 등
    val imageUrl: String,
    val userText: String? = null,
    val category: String,    // 소분류 (무드 태그: "새벽", "드라이브" 등)
    val mainTab: String,     // 대분류 ("MUSIC", "MOVIE", "BOOK", "SPACE", "FASHION", "VIBE")
    val globalTags: List<String> = emptyList(),
    val localTags: List<String> = emptyList()
)

// [BY MOOD] 그리드용 모델
data class MoodItem(
    val title: String,          // 무드 이름 (태그명)
    val recentImageUrl: String? // 해당 무드 아이템 중 가장 최근 이미지
)