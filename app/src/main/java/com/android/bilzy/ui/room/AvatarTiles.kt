package com.android.bilzy.ui.room

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import coil.transform.CircleCropTransformation
import com.android.bilzy.R
import com.android.bilzy.util.setFontWeight

/**
 * 멤버 대기/계산 대기 화면의 멤버 아바타 한 칸(피그마 "정산 인원 대기 중"/"계산중").
 * 47dp 원(테두리 3) + 우측 하단 20dp 체크 배지 + 아래 이름(14sp). [active]=false면 흐린 대기 스타일.
 * 6명까지는 피그마처럼 고정 폭(52dp, 간격 8dp)으로 가운데 정렬해 놓고, 그보다 많으면 [weighted]로 균등 분배한다.
 */
fun buildAvatarTile(
    ctx: Context,
    initial: String,
    label: String,
    active: Boolean,
    profileImageUrl: String? = null,
    weighted: Boolean = false
): View {
    fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    val tile = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
        clipChildren = false
        layoutParams = if (weighted) {
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        } else {
            LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        }
    }
    val circle = FrameLayout(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(dp(47), dp(47))
        setBackgroundResource(if (active) R.drawable.bg_avatar_done else R.drawable.bg_avatar_pending)
        clipChildren = false
        clipToPadding = false
    }
    circle.addView(TextView(ctx).apply {
        text = initial
        setTextColor(Color.parseColor(if (active) "#9E95E8" else "#5A5989"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setFontWeight(600)
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
    })
    if (!profileImageUrl.isNullOrBlank()) {
        val inset = dp(3)
        val avatarImage = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { setMargins(inset, inset, inset, inset) }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        circle.addView(avatarImage)
        // 이니셜을 아래 레이어로 남겨두고, 이미지 로드 실패 시 이 뷰만 숨겨 자연스럽게 폴백한다.
        avatarImage.load(profileImageUrl) {
            crossfade(true)
            transformations(CircleCropTransformation())
            listener(onError = { _, _ -> avatarImage.visibility = View.GONE })
        }
    }
    val badge = FrameLayout(ctx).apply {
        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.BOTTOM or Gravity.END).apply {
            bottomMargin = dp(3)
            marginEnd = -dp(5)
        }
        setBackgroundResource(R.drawable.bg_avatar_check_badge)
        backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (active) "#776AEC" else "#544E8A"))
    }
    badge.addView(ImageView(ctx).apply {
        setImageResource(R.drawable.ic_check)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        layoutParams = FrameLayout.LayoutParams(dp(10), dp(10), Gravity.CENTER)
    })
    circle.addView(badge)

    tile.addView(circle)
    tile.addView(TextView(ctx).apply {
        text = label
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL
        setTextColor(Color.parseColor(if (active) "#9E95E8" else "#5D5B8F"))
        // 이름이 길면("홍길동(나)" 등) 47dp 폭에 들어가도록 글자를 줄인다.
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length > 3) 11f else 14f)
        setFontWeight(500)
        layoutParams = LinearLayout.LayoutParams(dp(52), dp(24)).apply { topMargin = dp(3) }
    })
    return tile
}
