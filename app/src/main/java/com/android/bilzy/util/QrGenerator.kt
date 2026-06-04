package com.android.bilzy.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** 문자열을 QR 코드 비트맵으로 인코딩한다(ZXing). */
object QrGenerator {

    /**
     * [content]를 [size]px 정사각 QR 비트맵으로 만든다.
     * ML Kit(FORMAT_QR_CODE)로 다시 읽을 수 있는 표준 QR이다.
     */
    fun encode(
        content: String,
        size: Int = 512,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (matrix[x, y]) foreground else background
            }
        }
        val bitmap = createBitmap(size, size)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }
}
