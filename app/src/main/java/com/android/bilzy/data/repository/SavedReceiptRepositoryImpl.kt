package com.android.bilzy.data.repository

import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.toDomain
import com.android.bilzy.domain.model.SavedReceipt
import com.android.bilzy.domain.repository.SavedReceiptRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedReceiptRepositoryImpl @Inject constructor(
    private val api: BilzyApi
) : SavedReceiptRepository {

    override suspend fun getMyReceipts(): List<SavedReceipt> =
        api.getSavedReceipts().map { it.toDomain() }

    override suspend fun scan(imageBytes: ByteArray, mimeType: String): Long =
        api.scanSavedReceipt(filePart(imageBytes, mimeType)).total

    override suspend fun save(
        imageBytes: ByteArray,
        mimeType: String,
        storeName: String,
        totalAmount: Long
    ): SavedReceipt {
        val text = "text/plain".toMediaType()
        return api.saveReceipt(
            file = filePart(imageBytes, mimeType),
            storeName = storeName.toRequestBody(text),
            totalAmount = totalAmount.toString().toRequestBody(text)
        ).toDomain()
    }

    override suspend fun delete(id: String) = api.deleteSavedReceipt(id)

    private fun filePart(imageBytes: ByteArray, mimeType: String): MultipartBody.Part {
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        return MultipartBody.Part.createFormData(
            name = "file",
            filename = "receipt.$ext",
            body = imageBytes.toRequestBody(mimeType.toMediaType())
        )
    }
}
