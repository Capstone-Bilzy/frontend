package com.android.bilzy.data.repository

import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.AddItemRequest
import com.android.bilzy.data.remote.dto.OcrConfirmRequest
import com.android.bilzy.data.remote.dto.toDomain
import com.android.bilzy.data.remote.dto.toDto
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
        imageBytes: ByteArray,
        mimeType: String
    ): ScannedReceipt {
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = "receipt.$ext",
            body = imageBytes.toRequestBody(mimeType.toMediaType())
        )
        return api.scanReceipt(settlementId, part).toDomain()
    }

    override suspend fun confirm(settlementId: String, items: List<ReceiptItemDraft>): Long =
        api.confirmOcr(OcrConfirmRequest(settlementId, items.map { it.toDto() })).totalAmount

    override suspend fun addItem(
        settlementId: String,
        name: String,
        price: Long,
        quantity: Int
    ): ReceiptItem =
        api.addItem(AddItemRequest(settlementId, name, price, quantity)).toDomain()
}
