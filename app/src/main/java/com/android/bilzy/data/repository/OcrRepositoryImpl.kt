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
        val response = api.confirmOcr(
            OcrConfirmRequest(
                settlementId = settlementId,
                round = round,
                storeName = storeName,
                items = items.map { it.toDto() }
            )
        )
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
}
