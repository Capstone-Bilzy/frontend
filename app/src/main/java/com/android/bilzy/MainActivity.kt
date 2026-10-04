package com.android.bilzy

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import android.widget.Toast
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.databinding.ActivityMainBinding
import com.android.bilzy.util.JoinLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject lateinit var tokenStore: TokenStore

    /**
     * 화면을 디자인 기준 폭(피그마 390dp)에 맞춰 통째로 비례 축소/확대한다.
     * 폭이 더 좁은 폰(360dp 등)이나 시스템 글꼴을 키운 폰에서도 글자가 줄바꿈되거나 겹치지 않고
     * 디자인 비율 그대로 보이게 하기 위함. 시스템 글꼴 크기 설정은 반영하지 않는다(fontScale 고정).
     * 태블릿(최소 폭 600dp 이상)은 세로가 지나치게 좁아지므로 밀도는 건드리지 않는다.
     */
    override fun attachBaseContext(newBase: Context) {
        val metrics = newBase.resources.displayMetrics
        val shortSidePx = minOf(metrics.widthPixels, metrics.heightPixels)
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = 1f
        if (shortSidePx / metrics.density < 600f) {
            // 폭 기준 배율과, 세로가 짧은 폰(16:9 등)에서 최소 높이를 확보하는 배율 중 작은 쪽을 쓴다.
            // 세로가 짧으면 화면 전체가 조금 더 작게 그려져 아래 버튼과 내용이 겹치지 않는다.
            val longSidePx = maxOf(metrics.widthPixels, metrics.heightPixels)
            val scale = minOf(shortSidePx / DESIGN_WIDTH_DP, longSidePx / DESIGN_MIN_HEIGHT_DP)
            config.densityDpi = (scale * DisplayMetrics.DENSITY_DEFAULT).toInt()
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 런치 시 스플래시(Theme.Bilzy.Splash) → 콘텐츠 표시 전 일반 테마로 전환
        setTheme(R.style.Theme_Bilzy)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.parseColor("#020A2F")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        if (savedInstanceState == null) {
            // 초대 딥링크로 진입했으면 그쪽 흐름으로, 아니면 일반 진입 게이트.
            val invite = pendingJoinInvite()
            if (invite != null) {
                routeToJoin(invite)
            } else {
                lifecycleScope.launch {
                    if (tokenStore.isLoggedIn()) navigateToHome()
                }
            }
        }
    }

    /**
     * Android 15+(targetSdk 35+)는 edge-to-edge가 강제돼 콘텐츠가 내비게이션 바/키보드 밑으로 깔린다.
     * 상단은 각 화면이 상태바 여백을 직접 잡고 있으므로 하단(내비게이션 바·키보드)만 루트 패딩으로 비운다.
     */
    private fun applySystemBarInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(bottom = maxOf(bars.bottom, ime.bottom))
            insets
        }
    }

    /** 앱이 떠 있는 상태에서 딥링크가 새로 도착(singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingJoinInvite()?.let { routeToJoin(it) }
    }

    /**
     * 현재 인텐트가 유효한 초대 딥링크면 파싱 결과를 반환하고 인텐트를 소비한다.
     * 스킴/호스트가 다르거나 id가 UUID가 아니면 null(잘못된 링크는 조용히 무시).
     */
    private fun pendingJoinInvite(): JoinLink.ParsedInvite? {
        val data = intent?.data ?: return null
        val invite = JoinLink.parse(data.toString())
        // 재처리(회전/재진입) 방지: 한 번 읽으면 소비
        intent.data = null
        if (invite == null) {
            Toast.makeText(this, "유효하지 않은 초대 링크예요", Toast.LENGTH_SHORT).show()
            return null
        }
        return invite
    }

    /**
     * 초대 딥링크 라우팅. 로그인돼 있으면 입장 '확인' 화면으로(자동 가입 금지),
     * 아니면 로그인을 먼저 하도록 안내(가입은 로그인 사용자만).
     */
    private fun routeToJoin(invite: JoinLink.ParsedInvite) {
        lifecycleScope.launch {
            if (!tokenStore.isLoggedIn()) {
                Toast.makeText(
                    this@MainActivity,
                    "로그인 후 초대 링크를 다시 눌러주세요",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            // 홈을 베이스로 깔고(뒤로가기 시 홈), 그 위에 입장 확인 화면
            navigateToHome()
            navController.navigate(
                R.id.joinConfirmFragment,
                bundleOf("settlementId" to invite.settlementId, "token" to invite.token)
            )
        }
    }

    private fun navigateToHome() {
        if (navController.currentDestination?.id == R.id.homeFragment) return
        val options = NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, inclusive = true)
            .build()
        navController.navigate(R.id.homeFragment, null, options)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private companion object {
        /** 레이아웃을 만들 때 기준으로 삼은 화면 폭(dp). */
        const val DESIGN_WIDTH_DP = 390f

        /** 화면에 최소한 확보할 세로 길이(dp). 피그마 프레임은 844지만 시스템 바를 빼고도 내용이 들어가는 하한. */
        const val DESIGN_MIN_HEIGHT_DP = 800f
    }
}
