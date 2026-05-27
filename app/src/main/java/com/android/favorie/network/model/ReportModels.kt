package com.android.favorie.network.model

import com.google.gson.annotations.SerializedName

data class OverallReportResponse(
    @SerializedName("memberId")        val memberId: Long,
    @SerializedName("categoryVectors") val categoryVectors: List<CategoryVector>
)

data class CategoryVector(
    @SerializedName("category")  val category: String,
    @SerializedName("vector")    val vector: List<Double>,
    @SerializedName("updatedAt") val updatedAt: String
)

data class MonthlyReportResponse(
    @SerializedName("yearMonth")         val yearMonth: String,
    @SerializedName("diversityScore")    val diversityScore: Double,
    @SerializedName("rarityScore")       val rarityScore: Double,
    @SerializedName("badge")             val badge: String,
    @SerializedName("summary")           val summary: String,
    @SerializedName("recommendedItemId") val recommendedItemId: Long?
)
