package com.android.favorie.network

import com.android.favorie.network.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── AUTH ──────────────────────────────────────────────────────────────
    @POST("auth/kakao")
    suspend fun kakaoLogin(
        @Body request: KakaoLoginRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    // ── MEMBERS ───────────────────────────────────────────────────────────
    @GET("members/me")
    suspend fun getMyProfile(): Response<MemberResponse>

    @PATCH("members/me/nickname")
    suspend fun updateNickname(
        @Body request: NicknameUpdateRequest
    ): Response<MemberResponse>

    @DELETE("members/me")
    suspend fun deleteAccount(): Response<Unit>

    // ── HOME ──────────────────────────────────────────────────────────────
    @GET("home")
    suspend fun getHome(): Response<HomeResponse>

    // ── ITEMS 목록/상세 ────────────────────────────────────────────────────
    // category: MOVIE, BOOK, MUSIC, PLACE, FASHION, MOOD
    @GET("items")
    suspend fun getItems(
        @Query("category") category: String,
        @Query("cursor") cursor: Long? = null,
        @Query("size") size: Int = 20
    ): Response<ItemListResponse>

    @GET("items/{itemId}")
    suspend fun getItemDetail(
        @Path("itemId") itemId: Long
    ): Response<ItemResponse>

    @GET("items/{itemId}/status")
    suspend fun getItemStatus(
        @Path("itemId") itemId: Long
    ): Response<ItemStatusResponse>

    @DELETE("items/{itemId}")
    suspend fun deleteItem(
        @Path("itemId") itemId: Long
    ): Response<Unit>

    // ── ITEMS 저장 ─────────────────────────────────────────────────────────
    @POST("items/movies")
    suspend fun recordMovie(
        @Body request: MovieItemRequest
    ): Response<ItemResponse>

    @POST("items/books")
    suspend fun recordBook(
        @Body request: BookItemRequest
    ): Response<ItemResponse>

    @POST("items/fashions")
    suspend fun recordFashion(
        @Body request: FashionItemRequest
    ): Response<ItemResponse>

    @POST("items/moods")
    suspend fun recordMood(
        @Body request: MoodItemRequest
    ): Response<ItemResponse>

    @POST("items/places")
    suspend fun recordPlace(
        @Body request: PlaceItemRequest
    ): Response<ItemResponse>

    @POST("items/music")
    suspend fun recordMusic(
        @Body request: MusicItemRequest
    ): Response<ItemResponse>

    // ── CONTENTS 검색 ─────────────────────────────────────────────────────
    @GET("contents/movies/search")
    suspend fun searchMovies(
        @Query("q") query: String
    ): Response<MovieSearchResponse>

    @GET("contents/books/search")
    suspend fun searchBooks(
        @Query("q") query: String
    ): Response<BookSearchResponse>

    @GET("contents/movies/{tmdbId}")
    suspend fun getMovieDetail(
        @Path("tmdbId") tmdbId: Long
    ): Response<MovieDetail>

    @GET("contents/books/{bookId}")
    suspend fun getBookDetail(
        @Path("bookId") bookId: String
    ): Response<BookDetail>

    @GET("contents/musics/search")
    suspend fun searchMusics(
        @Query("q") query: String
    ): Response<MusicSearchResponse>

    @GET("contents/musics/{spotifyId}")
    suspend fun getMusicDetail(
        @Path("spotifyId") spotifyId: String
    ): Response<MusicDetail>

    // ── S3 이미지 업로드 ───────────────────────────────────────────────────
    // 1) presigned URL 발급 → 2) uploadUrl로 PUT 업로드 → 3) objectUrl을 imageUrl로 사용
    @GET("s3/presigned-url")
    suspend fun getPresignedUrl(
        @Query("filename") filename: String
    ): Response<PresignedUrlResponse>

    // ── REPORTS 취향 분석 ─────────────────────────────────────────────────
    @GET("reports/overall")
    suspend fun getOverallReport(): Response<OverallReportResponse>

    @GET("reports/monthly")
    suspend fun getMonthlyReport(
        @Query("yearMonth") yearMonth: String
    ): Response<MonthlyReportResponse>

    // ── RECOMMEND 추천 ────────────────────────────────────────────────────
    // 내가 스크랩한 추천 목록 (저장 화면용)
    @GET("recommend/my")
    suspend fun getMyRecommendations(): Response<MyRecommendResponse>

    // 카테고리별 일일 추천 (메타 미포함 — 상세는 /recommend/items/{itemId}로 조회)
    @GET("recommend/category")
    suspend fun getCategoryRecommendations(): Response<RecommendResponse>

    @GET("recommend/items/{itemId}")
    suspend fun getRecommendedItemDetail(
        @Path("itemId") itemId: Long
    ): Response<RecommendItemDetailResponse>

    // 추천 스크랩(북마크) — 이후 /recommend/my 응답에 포함된다
    @PATCH("recommend/{recommendationId}/scrap")
    suspend fun scrapRecommendation(
        @Path("recommendationId") recommendationId: Long
    ): Response<Unit>

    // POST /items/{category} 성공 직후 추천 목록에서 정리할 때 사용
    @DELETE("recommend/{recommendationId}")
    suspend fun deleteRecommendation(
        @Path("recommendationId") recommendationId: Long
    ): Response<Unit>

    // ── COMPARE 취향 궁합 ──────────────────────────────────────────────────
    @GET("compare/code")
    suspend fun getMyCompareCode(): Response<CompareCodeResponse>

    @GET("compare")
    suspend fun compare(
        @Query("code") code: String
    ): Response<CompareResponse>
}