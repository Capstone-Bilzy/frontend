package com.android.bilzy.util

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader

/** 로그아웃·탈퇴·로그인 만료 때 이전 계정의 프로필·영수증 사진 캐시(메모리·디스크)를 비운다. */
@OptIn(ExperimentalCoilApi::class)
fun clearImageCaches(context: Context) {
    val loader = context.applicationContext.imageLoader
    loader.memoryCache?.clear()
    runCatching { loader.diskCache?.clear() }
}
