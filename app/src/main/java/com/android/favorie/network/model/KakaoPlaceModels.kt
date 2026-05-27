package com.android.favorie.network.model

import com.google.gson.annotations.SerializedName

data class KakaoPlaceResponse(
    val documents: List<KakaoPlace>,
    val meta: KakaoMeta
)

data class KakaoPlace(
    val id: String,
    @SerializedName("place_name") val placeName: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("address_name") val addressName: String,
    @SerializedName("road_address_name") val roadAddressName: String,
    val phone: String,
    @SerializedName("place_url") val placeUrl: String,
    val x: String,  // longitude
    val y: String   // latitude
)

data class KakaoMeta(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("pageable_count") val pageableCount: Int,
    @SerializedName("is_end") val isEnd: Boolean
)