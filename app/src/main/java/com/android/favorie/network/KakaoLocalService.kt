package com.android.favorie.network

import com.android.favorie.network.model.KakaoPlaceResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoLocalService {

    @GET("v2/local/search/keyword.json")
    suspend fun searchPlaces(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("size") size: Int = 15
    ): Response<KakaoPlaceResponse>
}