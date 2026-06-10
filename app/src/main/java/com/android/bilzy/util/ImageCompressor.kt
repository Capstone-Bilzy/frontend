package com.android.bilzy.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 업로드(OCR 스캔) 전에 영수증 이미지를 다운스케일·재인코딩한다.
 * 카메라 원본(2~4MB)이나 갤러리 원본을 그대로 올리면 업로드 + 서버 Gemini 처리가 느려지므로,
 * 글자 인식에 충분한 해상도로 줄여 전송량과 처리 시간을 함께 낮춘다.
 */
object ImageCompressor {

    /** 긴 변 기준 최대 픽셀. 영수증 글자 인식엔 이 정도면 충분하다. */
    private const val MAX_DIMENSION = 1600

    /** JPEG 품질(0~100). 영수증은 글자만 읽으면 되므로 손실 무관. */
    private const val JPEG_QUALITY = 80

    /**
     * 압축 결과. 성공하면 항상 JPEG이므로 mime은 "image/jpeg".
     * 디코딩에 실패하거나 줄였더니 더 커지면 원본을 그대로 돌려준다.
     */
    data class Result(val bytes: ByteArray, val mime: String)

    /** 무거운 비트맵 작업이라 기본 디스패처에서 수행한다. */
    suspend fun compress(bytes: ByteArray, originalMime: String): Result =
        withContext(Dispatchers.Default) {
            runCatching { compressInternal(bytes) }
                .getOrNull()
                ?.takeIf { it.size < bytes.size }
                ?.let { Result(it, "image/jpeg") }
                ?: Result(bytes, originalMime)
        }

    private fun compressInternal(bytes: ByteArray): ByteArray {
        // 1) 경계만 읽어 원본 크기 파악
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return bytes

        // 2) inSampleSize로 메모리 효율적 다운샘플 후 디코딩
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(srcW, srcH, MAX_DIMENSION)
        }
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return bytes

        // 3) EXIF 회전 적용(카메라/갤러리 사진이 눕지 않도록 — OCR 정확도 영향)
        bitmap = applyExifOrientation(bytes, bitmap)

        // 4) MAX_DIMENSION에 맞춰 정확히 축소
        bitmap = scaleToMaxDimension(bitmap)

        // 5) JPEG 재인코딩
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim && h / 2 >= maxDim) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExifOrientation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it != bitmap) bitmap.recycle() }
    }

    private fun scaleToMaxDimension(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val ratio = MAX_DIMENSION.toFloat() / longest
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
            .also { if (it != bitmap) bitmap.recycle() }
    }
}
