package com.android.bilzy.data.repository

import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.AddItemRequest
import com.android.bilzy.data.remote.dto.OcrConfirmRequest
import com.android.bilzy.data.remote.dto.toDomain
import com.android.bilzy.data.remote.dto.toDto
import com.android.bilzy.domain.model.OcrConfirmResult
import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.ReceiptItemDraft
import com.android.bilzy.domain.model.ScannedReceipt
import com.android.bilzy.domain.repository.OcrRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val api: BilzyApi
) : OcrRepository {

    override suspend fun scan(
        settlementId: String,
        round: Int,
        imageBytes: ByteArray,
        mimeType: String
    ): ScannedReceipt {
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = "receipt.$ext",
            body = imageBytes.toRequestBody(mimeType.toMediaType())
        )
        return api.scanReceipt(settlementId, round, part).toDomain()
    }

    override suspend fun confirm(
        settlementId: String,
        round: Int,
        storeName: String,
        items: List<ReceiptItemDraft>
    ): OcrConfirmResult {
        suspend fun send(list: List<ReceiptItemDraft>) = api.confirmOcr(
            OcrConfirmRequest(
                settlementId = settlementId,
                round = round,
                storeName = storeName,
                items = list.map { it.toDto() }
            )
        )
        var response = send(items)
        // 구버전 서버는 line_amount를 무시하고 단가×수량으로 저장해 합계가 1원씩 틀어진다(3개 10,000원 → 9,999원).
        // 서버가 돌려준 합계가 화면 합계와 다르면 예전 방식(금액×1개)으로 한 번 더 확정해 금액을 맞춘다.
        if (response.totalAmount != items.sumOf { it.subtotal } && items.any { it.lineAmount != null }) {
            response = send(items.map { it.normalized() })
        }
        return OcrConfirmResult(
            round = response.round,
            totalAmount = response.totalAmount,
            settlementTotalAmount = response.settlementTotalAmount
        )
    }

    override suspend fun addItem(
        settlementId: String,
        round: Int,
        name: String,
        price: Long,
        quantity: Int
    ): ReceiptItem =
        api.addItem(AddItemRequest(settlementId, round, name, price, quantity)).toDomain()

    override suspend fun attachPhoto(settlementId: String, imageBytes: ByteArray, mimeType: String) {
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = "receipt.$ext",
            body = imageBytes.toRequestBody(mimeType.toMediaType())
        )
        api.attachReceiptPhoto(settlementId, part)
    }
}
