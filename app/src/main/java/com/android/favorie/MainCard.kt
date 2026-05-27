package com.android.favorie

data class MainCard(
    val name: String,
    val color: String,
    val count: Int,
    val iconRes: Int,
    val cardImageRes: Int,
    val isAddButton: Boolean = false,
    val items: List<MusicItem> = emptyList()  // 서버에서 받아온 최근 아이템
)
