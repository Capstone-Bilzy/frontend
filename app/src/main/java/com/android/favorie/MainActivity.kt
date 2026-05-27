package com.android.favorie

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private var currentFragment: Fragment? = null
    private var homeExpandedCategory: String? = null  // 홈 카드 확장 시 선택된 카테고리

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)

// ⭐ 핵심: 툴바에만 시스템 상단바 높이만큼 패딩 추가
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // 왼쪽, 오른쪽, 아래 패딩은 기존 툴바 설정을 유지하고,
            // 위쪽(top)만 상단바 높이만큼 더해줍니다.
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)

            insets
        }

        drawerLayout = findViewById(R.id.drawer_layout)

        // 2. 만약 drawerLayout이 null이 아닐 때만 리스너를 설정합니다. (Null 안전성)
        drawerLayout?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 3. 이후 기존 로직 계속 진행
        val navView = findViewById<NavigationView>(R.id.nav_view)
        fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)

        // 툴바 클릭 시 드로어 열기
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 앱 실행 시 첫 화면 설정 (HomeFragment)
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), false)
        }

        // 아이템 추가 FAB 버튼 설정
        fabAdd.setOnClickListener {
            val category = when (currentFragment) {
                is MusicMainFragment   -> "MUSIC"
                is MovieMainFragment   -> "MOVIE"
                is BookMainFragment    -> "BOOK"
                is SpaceMainFragment   -> "SPACE"
                is FashionMainFragment -> "FASHION"
                is VibeMainFragment    -> "MOOD"
                is HomeFragment        -> homeExpandedCategory  // 홈에서 카드 확장 시 해당 카테고리 사용
                else                   -> null
            }
            replaceFragment(AddItemFragment.newInstance(category), true)
        }

        // NavigationView 내부의 메뉴 텍스트뷰 연결
        val menuHome = navView.findViewById<TextView>(R.id.menu_home)
        val menuAnalysis = navView.findViewById<TextView>(R.id.menu_analysis)
        val menuRecommend = navView.findViewById<TextView>(R.id.menu_recommend)
        val menuSavedRecommend = navView.findViewById<TextView>(R.id.menu_saved_recommend)
        val menuMyPage = navView.findViewById<TextView>(R.id.menu_mypage)
        val menuShare = navView.findViewById<TextView>(R.id.menu_share)

        // 각 메뉴 클릭 리스너 설정
        menuHome?.setOnClickListener {
            replaceFragment(HomeFragment(), false)
        }

        menuAnalysis?.setOnClickListener {
            replaceFragment(AnalysisFragment(), true)
        }

        menuRecommend?.setOnClickListener {
            replaceFragment(RecommendMainFragment(), true)
        }

        menuSavedRecommend?.setOnClickListener {
            replaceFragment(SavedRecommendFragment(), true)
        }

        menuMyPage?.setOnClickListener {
            replaceFragment(MyPageFragment(), true)
        }

        menuShare?.setOnClickListener {
            replaceFragment(CompatibilityFragment(), true)
        }

// 뒤로가기 콜백 설정
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. 드로어가 열려 있으면 닫기
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // 2. 백스택에 프래그먼트가 있으면 뒤로 가기
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                    } else {
                        // 3. 더 이상 뒤로 갈 곳이 없으면 앱 종료 (기본 동작)
                        isEnabled = false // 콜백 잠시 비활성화
                        onBackPressedDispatcher.onBackPressed() // 다시 호출
                        isEnabled = true // 다시 활성화
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        // 뒤로가기로 복귀 시 FAB 상태 및 툴바 뒤로가기 버튼 복원
        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            currentFragment = current
            updateFab(current)
            updateToolbarNavigation()
        }
    }

    private fun updateToolbarNavigation() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left)
            toolbar.navigationIcon?.setTint(Color.WHITE)
            toolbar.setNavigationOnClickListener { supportFragmentManager.popBackStack() }
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_menu)
            toolbar.navigationIcon?.setTint(Color.WHITE)
            toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        }
    }

    fun setExpandedCategory(category: String?) {
        homeExpandedCategory = category
    }

    fun navigateCategoryFragment(fragment: Fragment) {
        homeExpandedCategory = null  // 카테고리 페이지로 이동 시 홈 확장 상태 초기화
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
        currentFragment = fragment
        updateFab(fragment)
        updateToolbarNavigation()
    }

    private fun updateFab(fragment: Fragment?) {
        val iconRes = when (fragment) {
            is MusicMainFragment   -> R.drawable.ic_add_yellow
            is MovieMainFragment   -> R.drawable.ic_add_red
            is BookMainFragment    -> R.drawable.ic_add_green
            is SpaceMainFragment   -> R.drawable.ic_add_orange
            is FashionMainFragment -> R.drawable.ic_add_blue
            is VibeMainFragment    -> R.drawable.ic_add_purple
            is HomeFragment        -> R.drawable.ic_add
            else                   -> null
        }
        if (iconRes != null) {
            fabAdd.setImageResource(iconRes)
            // 홈은 검정 tint 유지, 카테고리 아이콘은 원래 색상 그대로 표시
            if (fragment is HomeFragment) {
                fabAdd.imageTintList = ColorStateList.valueOf(Color.BLACK)
            } else {
                fabAdd.imageTintList = null
            }
            fabAdd.show()
        } else {
            fabAdd.hide()
        }
    }

    /**
     * 프래그먼트 교체 공통 함수
     * @param fragment 교체할 프래그먼트 객체
     * @param addToBackStack 뒤로가기 버튼 유지 여부
     */
    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()

        // 자연스러운 화면 전환을 위한 애니메이션 (선택사항)
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

        transaction.replace(R.id.nav_host_fragment, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()

        currentFragment = fragment
        updateFab(fragment)

        // 메뉴 이동 후 드로어 닫기
        drawerLayout.closeDrawer(GravityCompat.START)
    }
}