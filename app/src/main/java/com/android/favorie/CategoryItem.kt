package com.android.favorie

data class CategoryItem(
    val id: Long,               // DB의 id
    val title: String,           // 아이템 제목 또는 무드 이름
    val imageUrl: String?,       // 최근 추가된 아이템의 이미지 URL (앨범아트, 포스터 등)
    val category: String,        // 'music', 'movie', 'book' 등 구분용
    val subText: String? = null  // (선택) 가수명이나 저자 등 부가 정보
)