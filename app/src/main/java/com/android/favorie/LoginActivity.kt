package com.android.favorie

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 테마를 원래 앱 테마로 바로 바꿔줌 (안 그러면 계속 배경이미지가 남음)
        setTheme(R.style.Theme_Favorie)

        super.onCreate(savedInstanceState)

        // 1. Edge-to-Edge 설정 (상태바를 투명하게 하고 배경을 꽉 채움)
        // SystemBarStyle.dark를 사용하면 상태바 아이콘이 자동으로 흰색 계열로 설정됩니다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContentView(R.layout.activity_login)

        // 2. 상태바 아이콘 색상 강제 지정 (배경이 검은색이니 아이콘은 흰색으로!)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false

        // 3. 첫 진입 시 LoginFragment 호출
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.login_container, LoginFragment())
                .commit()
        }
    }
}