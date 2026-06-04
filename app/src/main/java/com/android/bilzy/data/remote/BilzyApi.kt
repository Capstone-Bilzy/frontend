package com.android.bilzy.data.remote

import com.android.bilzy.data.remote.dto.AddItemRequest
import com.android.bilzy.data.remote.dto.AddMemberRequest
import com.android.bilzy.data.remote.dto.AuthResponse
import com.android.bilzy.data.remote.dto.CreateSettlementRequest
import com.android.bilzy.data.remote.dto.HistoryDto
import com.android.bilzy.data.remote.dto.OcrConfirmRequest
import com.android.bilzy.data.remote.dto.OcrConfirmResponse
import com.android.bilzy.data.remote.dto.OcrScanResponse
import com.android.bilzy.data.remote.dto.ReceiptItemDto
import com.android.bilzy.data.remote.dto.RefreshRequest
import com.android.bilzy.data.remote.dto.SettlementDto
import com.android.bilzy.data.remote.dto.SettlementMemberDto
import com.android.bilzy.data.remote.dto.SocialLoginRequest
import com.android.bilzy.data.remote.dto.UpdateSettlementRequest
import com.android.bilzy.data.remote.dto.UserDto
import com.android.bilzy.data.remote.dto.UpdateStatusRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Bilzy 백엔드(FastAPI) REST 엔드포인트.
 * 기능을 하나씩 옮길 때마다 여기에 엔드포인트를 추가한다.
 */
interface BilzyApi {

    // ── 인증 ─────────────────────────────────────────────
    @POST("auth/social")
    suspend fun socialLogin(@Body body: SocialLoginRequest): AuthResponse

    // ── 유저 ─────────────────────────────────────────────
    @GET("users/me")
    suspend fun getMe(): UserDto

    @GET("users/me/history")
    suspend fun getHistory(): List<HistoryDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @HTTP(method = "DELETE", path = "auth/logout", hasBody = false)
    suspend fun logout()

    // ── 정산방 ───────────────────────────────────────────
    @POST("settlements")
    suspend fun createSettlement(@Body body: CreateSettlementRequest): SettlementDto

    @GET("settlements/{id}")
    suspend fun getSettlement(@Path("id") id: String): SettlementDto

    @PATCH("settlements/{id}")
    suspend fun updateSettlement(
        @Path("id") id: String,
        @Body body: UpdateSettlementRequest
    ): SettlementDto

    @PATCH("settlements/{id}/status")
    suspend fun updateSettlementStatus(
        @Path("id") id: String,
        @Body body: UpdateStatusRequest
    ): SettlementDto

    @DELETE("settlements/{id}")
    suspend fun deleteSettlement(@Path("id") id: String)

    /** QR로 정산방 참여. 본인 닉네임으로 멤버 추가. */
    @POST("settlements/{id}/join")
    suspend fun joinSettlement(
        @Path("id") id: String,
        @Body body: AddMemberRequest
    ): SettlementMemberDto

    /** AI(Gemini) 정산 계산. 멤버별 금액·사유는 서버에 저장되고, 이후 GET 상세로 받는다. */
    @POST("settlements/{id}/calculate")
    suspend fun calculateSplit(
        @Path("id") id: String,
        @Body body: com.android.bilzy.data.remote.dto.CalculateRequest
    ): com.android.bilzy.data.remote.dto.CalculateResultDto

    /** 정산 완료 처리(status=done, 내역 기록). */
    @POST("settlements/{id}/done")
    suspend fun markSettlementDone(@Path("id") id: String): SettlementDto

    // ── OCR ──────────────────────────────────────────────
    /** 영수증 이미지 업로드 → 서버(Gemini)가 OCR. settlement_id는 쿼리 파라미터. */
    @Multipart
    @POST("ocr/scan")
    suspend fun scanReceipt(
        @Query("settlement_id") settlementId: String,
        @Part file: MultipartBody.Part
    ): OcrScanResponse

    @POST("ocr/confirm")
    suspend fun confirmOcr(@Body body: OcrConfirmRequest): OcrConfirmResponse

    @POST("ocr/add-item")
    suspend fun addItem(@Body body: AddItemRequest): ReceiptItemDto
}
