package com.android.bilzy.ui.common

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 키보드가 올라와 있는 동안 하단 버튼을 숨긴다 — 화면이 키보드만큼 줄어들 때 버튼이 키보드 위로
 * 따라 올라와 입력 칸을 가리지 않게. onViewCreated에서 한 번 부르면 뷰가 사라질 때 알아서 해제된다.
 * 넘기는 뷰는 평소 항상 보이는 것(버튼 하나 또는 버튼 줄 전체)이어야 한다.
 */
fun Fragment.hideWhileKeyboardShown(vararg views: View) {
    val root = view ?: return
    val watcher = ViewTreeObserver.OnGlobalLayoutListener {
        val imeVisible = ViewCompat.getRootWindowInsets(root)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        val target = if (imeVisible) View.GONE else View.VISIBLE
        views.forEach { if (it.visibility != target) it.visibility = target }
    }
    root.viewTreeObserver.addOnGlobalLayoutListener(watcher)
    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            root.viewTreeObserver.removeOnGlobalLayoutListener(watcher)
        }
    })
}
