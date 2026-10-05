package com.android.bilzy.util

/**
 * 카카오는 프로필 사진 주소를 `http://k.kakaocdn.net/...`처럼 http로 내려주는데, 앱은 보안 설정상
 * 평문(http) 통신을 막아 두어 그대로는 이미지가 로드되지 않는다(네이버는 https라 문제없음).
 * 같은 주소가 https로도 제공되므로 로드 전에 https로 바꾼다.
 */
fun String.toHttpsUrl(): String =
    if (startsWith("http://")) "https://" + removePrefix("http://") else this
