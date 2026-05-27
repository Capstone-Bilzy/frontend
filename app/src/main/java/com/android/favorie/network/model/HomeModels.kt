package com.android.favorie.network.model

import com.google.gson.annotations.SerializedName

// GET /home 응답 - 카테고리별 아이템 수
data class HomeResponse(
    @SerializedName("music") val music: Int,
    @SerializedName("movie") val movie: Int,
    @SerializedName("book") val book: Int,
    @SerializedName("place") val space: Int,
    @SerializedName("fashion") val fashion: Int,
    @SerializedName("mood") val vibe: Int
)