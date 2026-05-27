package com.android.favorie

import android.app.Application
import com.android.favorie.TokenManager
import com.kakao.sdk.common.KakaoSdk

class FavorieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
        TokenManager.init(this)
    }
}