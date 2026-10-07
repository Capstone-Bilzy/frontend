package com.android.bilzy.ui.common

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.bilzy.R
import java.util.WeakHashMap

/**
 * 서버 응답을 기다리는 동안 화면 가운데에 띄우는 작은 로딩 표시.
 *
 * 서버(Render 무료 등급)가 느릴 때 요청 하나에 수십 초가 걸리기도 하는데, 그동안 버튼만 비활성화돼 있어
 * 앱이 멈춘 것처럼 보였다. 화면을 막지는 않는다(포커스·터치를 가져가지 않음) — 뒤로가기는 그대로 되고,
 * 중복 요청은 기존처럼 각 화면이 버튼을 비활성화해서 막는다.
 * 금방 끝나는 요청에서는 깜빡이지 않도록 [SHOW_DELAY_MS]가 지난 뒤에만 나타난다.
 */
class LoadingIndicator(private val fragment: Fragment, owner: LifecycleOwner) : DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private var dialog: Dialog? = null
    private val showRunnable = Runnable { present() }

    init {
        owner.lifecycle.addObserver(this)
    }

    fun show() {
        if (dialog != null) return
        handler.removeCallbacks(showRunnable)
        handler.postDelayed(showRunnable, SHOW_DELAY_MS)
    }

    fun hide() {
        handler.removeCallbacks(showRunnable)
        dialog?.let { runCatching { it.dismiss() } }
        dialog = null
    }

    /** 조건에 따라 켜고 끈다(상태 Flow를 그대로 물릴 때 편함). */
    fun set(loading: Boolean) = if (loading) show() else hide()

    private fun present() {
        val ctx = fragment.context ?: return
        if (!fragment.isAdded || dialog != null) return
        val density = ctx.resources.displayMetrics.density
        val box = (72 * density).toInt()
        val spinner = (36 * density).toInt()
        val card = FrameLayout(ctx).apply {
            setBackgroundResource(R.drawable.bg_terms_dialog)
            addView(
                ProgressBar(ctx).apply {
                    isIndeterminate = true
                    indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#AAB2FF"))
                },
                FrameLayout.LayoutParams(spinner, spinner, Gravity.CENTER)
            )
        }
        dialog = Dialog(ctx).apply {
            setContentView(card, ViewGroup.LayoutParams(box, box))
            setCancelable(false)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(box, box)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
            }
            runCatching { show() }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        hide()
        indicators.remove(owner)
    }

    companion object {
        private const val SHOW_DELAY_MS = 400L
        private val indicators = WeakHashMap<LifecycleOwner, LoadingIndicator>()

        internal fun of(fragment: Fragment): LoadingIndicator {
            val owner = fragment.viewLifecycleOwner
            return indicators.getOrPut(owner) { LoadingIndicator(fragment, owner) }
        }
    }
}

/** 이 화면(뷰)의 로딩 표시. 화면이 사라지면 자동으로 내려간다. onViewCreated 이후에만 쓸 것. */
val Fragment.loading: LoadingIndicator get() = LoadingIndicator.of(this)
