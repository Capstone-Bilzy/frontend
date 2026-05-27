package com.android.favorie.network.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CompareCodeResponse(
    @SerializedName("code") val code: String
)

data class CompareResponse(
    @SerializedName("me")                   val me: CompareMember,
    @SerializedName("opponent")             val opponent: CompareMember,
    @SerializedName("overallSimilarity")    val overallSimilarity: Double,
    @SerializedName("categorySimilarities") val categorySimilarities: List<CategorySimilarity>,
    @SerializedName("commonTags")           val commonTags: List<CommonTag>
) : Serializable

data class CompareMember(
    @SerializedName("memberId") val memberId: Long,
    @SerializedName("nickname") val nickname: String
) : Serializable

data class CategorySimilarity(
    @SerializedName("category")   val category: String,
    @SerializedName("similarity") val similarity: Double
) : Serializable

data class CommonTag(
    @SerializedName("id")   val id: Long,
    @SerializedName("name") val name: String
) : Serializable