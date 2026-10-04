package com.android.bilzy.util

import android.graphics.Typeface
import android.os.Build
import android.widget.TextView

/**
 * 코드로 만드는 TextView의 굵기를 피그마 값(300~700)으로 맞춘다.
 * 앱 기본 글꼴(Pretendard, 테마에서 지정)은 그대로 두고 굵기만 바꾼다.
 * API 28 미만은 세밀한 굵기를 고를 수 없어 600 이상만 굵게 처리한다.
 */
fun TextView.setFontWeight(weight: Int) {
    typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Typeface.create(typeface, weight, false)
    } else {
        Typeface.create(typeface, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
    }
}
