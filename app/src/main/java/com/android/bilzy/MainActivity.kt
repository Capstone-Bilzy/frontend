package com.android.bilzy

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // 런치 시 스플래시(Theme.Bilzy.Splash) → 콘텐츠 표시 전 일반 테마로 전환
        setTheme(R.style.Theme_Bilzy)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.parseColor("#0A1130")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        if (savedInstanceState == null) {
            // 초대 딥링크로 진입했으면 그쪽 흐름으로, 아니면 일반 진입 게이트.
            val joinId = pendingJoinId()
            if (joinId != null) {
                routeToJoin(joinId)
            } else {
                lifecycleScope.launch {
                    if (tokenStore.isLoggedIn()) navigateToHome()
                }
            }
        }
    }

    /** 앱이 떠 있는 상태에서 딥링크가 새로 도착(singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingJoinId()?.let { routeToJoin(it) }
    }

    /**
     * 현재 인텐트가 유효한 초대 딥링크면 settlement_id(UUID)를 반환하고 인텐트를 소비한다.
     * 스킴/호스트가 다르거나 id가 UUID가 아니면 null(잘못된 링크는 조용히 무시).
     */
    private fun pendingJoinId(): String? {
        val data = intent?.data ?: return null
        if (data.scheme != JoinLink.SCHEME || data.host != JoinLink.HOST) return null
        val id = data.lastPathSegment
        // 재처리(회전/재진입) 방지: 한 번 읽으면 소비
        intent.data = null
        if (!JoinLink.isValidId(id)) {
            Toast.makeText(this, "유효하지 않은 초대 링크예요", Toast.LENGTH_SHORT).show()
            return null
        }
        return id
    }

    /**
     * 초대 딥링크 라우팅. 로그인돼 있으면 입장 '확인' 화면으로(자동 가입 금지),
     * 아니면 로그인을 먼저 하도록 안내(가입은 로그인 사용자만).
     */
    private fun routeToJoin(settlementId: String) {
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
                bundleOf("settlementId" to settlementId)
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
}
