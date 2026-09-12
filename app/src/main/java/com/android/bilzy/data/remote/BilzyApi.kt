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
import com.android.bilzy.data.remote.dto.SettlementMemberRoundDto
import com.android.bilzy.data.remote.dto.SetMemberRoundsRequest
import com.android.bilzy.data.remote.dto.SetMemberRoundsResponse
import com.android.bilzy.data.remote.dto.SetRoundAdjustmentRequest
import com.android.bilzy.data.remote.dto.SocialLoginRequest
import com.android.bilzy.data.remote.dto.UpdateSettlementRequest
import com.android.bilzy.data.remote.dto.UserDto
import com.android.bilzy.data.remote.dto.UpdateStatusRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    /** 대표 계좌 조회. 신규 유저는 세 필드가 빈 문자열. */
    @GET("users/me/account")
    suspend fun getAccount(): com.android.bilzy.data.remote.dto.AccountDto

    /** 대표 계좌 저장/수정(upsert). 계좌번호는 서버에서 AES-256 암호화 저장. */
    @POST("users/me/account")
    suspend fun saveAccount(
        @Body body: com.android.bilzy.data.remote.dto.AccountRequest
    ): com.android.bilzy.data.remote.dto.AccountDto

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

    /** 본인이 참여한 라운드 집합을 통째로 교체(토글 결과 전체 전송, 서버가 diff 적용). */
    @PATCH("settlements/{id}/members/me/rounds")
    suspend fun setMyRounds(
        @Path("id") id: String,
        @Body body: SetMemberRoundsRequest
    ): SetMemberRoundsResponse

    /** 본인이 그 라운드에서 안 먹은 항목(항목명)을 통째로 교체. */
    @PATCH("settlements/{id}/members/me/rounds/{round}")
    suspend fun setMyRoundAdjustment(
        @Path("id") id: String,
        @Path("round") round: Int,
        @Body body: SetRoundAdjustmentRequest
    ): SettlementMemberRoundDto

    /** 본인이 금액 조정을 마치고 "정산 시작하기"를 눌렀음을 표시(CalculatingFragment 실시간 표시용). */
    @PATCH("settlements/{id}/members/me/ready")
    suspend fun setMyReady(@Path("id") id: String): SettlementMemberDto

    // ── OCR ──────────────────────────────────────────────
    /** 영수증 이미지 업로드 → 서버(Gemini)가 OCR. settlement_id·round는 쿼리 파라미터. */
    @Multipart
    @POST("ocr/scan")
    suspend fun scanReceipt(
        @Query("settlement_id") settlementId: String,
        @Query("round") round: Int,
        @Part file: MultipartBody.Part
    ): OcrScanResponse

    @POST("ocr/confirm")
    suspend fun confirmOcr(@Body body: OcrConfirmRequest): OcrConfirmResponse

    @POST("ocr/add-item")
    suspend fun addItem(@Body body: AddItemRequest): ReceiptItemDto

    /**
     * 정산건의 특정 라운드에 붙은 영수증 이미지 삭제(저장 안 함 선택 시). round 기본값 1.
     */
    @DELETE("settlements/{id}/receipt")
    suspend fun deleteSettlementReceipt(
        @Path("id") id: String,
        @Query("round") round: Int = 1
    )
}
