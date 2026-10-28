package com.android.bilzy.util

/**
 * 카카오는 프로필 사진 주소를 `http://k.kakaocdn.net/...`처럼 http로 내려주는데, 앱은 보안 설정상
 * 평문(http) 통신을 막아 두어 그대로는 이미지가 로드되지 않는다(네이버는 https라 문제없음).
 * 같은 주소가 https로도 제공되므로 로드 전에 https로 바꾼다.
 */
fun String.toHttpsUrl(): String =
    if (startsWith("http://")) "https://" + removePrefix("http://") else this

/**
 * 서버가 주는 사진 주소는 서명 URL이라 응답마다 `?token=...`만 바뀐다. 그대로 쓰면 같은 사진도 매번 새 이미지로
 * 취급돼 다시 받아오고, 그동안 빈 칸·기본 이미지가 잠깐 보인다. 캐시 키를 쿼리 없는 경로로 고정한다
 * (사진을 바꾸면 서버가 새 파일 이름으로 저장하므로 경로가 같으면 같은 사진이다).
 */
fun coil.request.ImageRequest.Builder.stableCacheKey(url: String): coil.request.ImageRequest.Builder {
    val key = url.substringBefore('?')
    return memoryCacheKey(key).diskCacheKey(key)
}
