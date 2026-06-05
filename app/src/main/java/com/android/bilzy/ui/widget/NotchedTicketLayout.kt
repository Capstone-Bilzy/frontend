package com.android.bilzy.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import com.android.bilzy.R

/**
 * 티켓 모양 카드. 프로스티드 글래스(반투명 + 흰 베일 + 라벤더 테두리)를 직접 그려서
 * 반원 컷(노치) 안쪽으로 뒤 배경 그라데이션이 비치게 한다.
 *
 * notchMode:
 *  - sideDivider(0): 좌우 가장자리에 반원 컷, 그 사이를 점선으로 연결. 노치 y = 첫 자식(상단 섹션) 하단 경계.
 *  - topCenter(1): 윗면 중앙에 반원 컷 하나.
 */
class NotchedTicketLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val density = resources.displayMetrics.density
    private val cornerRadius = 24f * density
    private val notchRadius = 13f * density

    private var notchMode = MODE_SIDE

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#59544FB0")
    }
    private val veilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1FFFFFFF")
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.parseColor("#80A5A6F6")
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#66FFFFFF")
        pathEffect = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
    }

    private val path = Path()

    init {
        setWillNotDraw(false)
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.NotchedTicketLayout)
            notchMode = a.getInt(R.styleable.NotchedTicketLayout_notchMode, MODE_SIDE)
            a.recycle()
        }
    }

    /** 측면 노치 y = 상단 섹션(첫 자식) 하단 경계 */
    private fun sideNotchY(): Float =
        if (childCount >= 1) getChildAt(0).bottom.toFloat() else height / 2f

    private fun buildPath() {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = cornerRadius
        val nr = notchRadius
        path.reset()

        if (notchMode == MODE_TOP) {
            // 윗면 중앙 반원 컷
            val cx = w / 2f
            path.moveTo(r, 0f)
            path.lineTo(cx - nr, 0f)
            path.arcTo(RectF(cx - nr, -nr, cx + nr, nr), 180f, -180f)   // 아래로 오목
            path.lineTo(w - r, 0f)
            path.arcTo(RectF(w - 2 * r, 0f, w, 2 * r), -90f, 90f)
            path.lineTo(w, h - r)
            path.arcTo(RectF(w - 2 * r, h - 2 * r, w, h), 0f, 90f)
            path.lineTo(r, h)
            path.arcTo(RectF(0f, h - 2 * r, 2 * r, h), 90f, 90f)
            path.lineTo(0f, r)
            path.arcTo(RectF(0f, 0f, 2 * r, 2 * r), 180f, 90f)
            path.close()
            return
        }

        // 좌우 측면 노치
        val ny = sideNotchY()
        path.moveTo(r, 0f)
        path.lineTo(w - r, 0f)
        path.arcTo(RectF(w - 2 * r, 0f, w, 2 * r), -90f, 90f)         // 우상단 코너
        path.lineTo(w, ny - nr)
        path.arcTo(RectF(w - nr, ny - nr, w + nr, ny + nr), -90f, -180f)  // 우측 노치
        path.lineTo(w, h - r)
        path.arcTo(RectF(w - 2 * r, h - 2 * r, w, h), 0f, 90f)
        path.lineTo(r, h)
        path.arcTo(RectF(0f, h - 2 * r, 2 * r, h), 90f, 90f)
        path.lineTo(0f, ny + nr)
        path.arcTo(RectF(-nr, ny - nr, nr, ny + nr), 90f, -180f)      // 좌측 노치
        path.lineTo(0f, r)
        path.arcTo(RectF(0f, 0f, 2 * r, 2 * r), 180f, 90f)
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        buildPath()
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, veilPaint)
        canvas.drawPath(path, borderPaint)
        if (notchMode == MODE_SIDE) {
            val ny = sideNotchY()
            val gap = notchRadius + 5f * density
            canvas.drawLine(gap, ny, width - gap, ny, dashPaint)
        }
    }

    companion object {
        private const val MODE_SIDE = 0
        private const val MODE_TOP = 1
    }
}
