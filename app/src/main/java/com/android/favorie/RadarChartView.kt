package com.android.favorie

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init { setWillNotDraw(false) }

    private val labels = listOf("MUSIC", "VIBE", "SPACE", "FASHION", "BOOK", "MOVIE")

    // 0.0~1.0 정규화 값 (최근 5개 - 보라색)
    private var dataValues = FloatArray(6) { 0f }
    // 0.0~1.0 정규화 값 (이전 5개 - 노란색)
    private var previousDataValues = FloatArray(6) { 0f }
    // 꼭짓점 개수 라벨
    private var rawCounts = IntArray(6) { 0 }

    private val gridCount = 4

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFDD57")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99BB3FBD")
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BB3FBD")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFDD57")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val prevFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FFDD57")
        style = Paint.Style.FILL
    }
    private val prevStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFDD57")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val prevDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFDD57")
        style = Paint.Style.FILL
    }

    // 최근 5개(보라) vs 이전 5개(노란) 비교 표시
    fun setCompareData(currentCounts: IntArray, previousCounts: IntArray) {
        rawCounts = currentCounts
        val currMax = currentCounts.maxOrNull()?.takeIf { it > 0 } ?: 1
        dataValues = FloatArray(currentCounts.size) { currentCounts[it].toFloat() / currMax }
        val prevMax = previousCounts.maxOrNull()?.takeIf { it > 0 } ?: 1
        previousDataValues = FloatArray(previousCounts.size) { previousCounts[it].toFloat() / prevMax }
        invalidate()
    }

    fun setCountData(counts: IntArray) {
        rawCounts = counts
        val max = counts.maxOrNull()?.takeIf { it > 0 } ?: 1
        dataValues = FloatArray(counts.size) { counts[it].toFloat() / max }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) * 0.55f
        val labelRadius = radius + 60f
        val n = labels.size
        val angleStep = (2.0 * Math.PI / n)
        val startAngle = -Math.PI / 2.0

        // 1. 배경 격자
        for (i in 1..gridCount) {
            val r = radius * i / gridCount
            val path = Path()
            for (j in 0 until n) {
                val angle = startAngle + j * angleStep
                val x = (cx + r * cos(angle)).toFloat()
                val y = (cy + r * sin(angle)).toFloat()
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, gridPaint)
        }

        // 2. 축 선
        for (j in 0 until n) {
            val angle = startAngle + j * angleStep
            val x = (cx + radius * cos(angle)).toFloat()
            val y = (cy + radius * sin(angle)).toFloat()
            canvas.drawLine(cx, cy, x, y, axisPaint)
        }

        // 3. 이전 5개 영역 (노란색) - 보라색 아래에 먼저 그림
        if (previousDataValues.any { it > 0f }) {
            val prevPath = Path()
            for (j in 0 until n) {
                val angle = startAngle + j * angleStep
                val r = radius * previousDataValues[j]
                val x = (cx + r * cos(angle)).toFloat()
                val y = (cy + r * sin(angle)).toFloat()
                if (j == 0) prevPath.moveTo(x, y) else prevPath.lineTo(x, y)
            }
            prevPath.close()
            canvas.drawPath(prevPath, prevFillPaint)
            canvas.drawPath(prevPath, prevStrokePaint)
            for (j in 0 until n) {
                val angle = startAngle + j * angleStep
                val r = radius * previousDataValues[j]
                val x = (cx + r * cos(angle)).toFloat()
                val y = (cy + r * sin(angle)).toFloat()
                canvas.drawCircle(x, y, 5f, prevDotPaint)
            }
        }

        // 4. 최근 5개 영역 (보라색)
        val dataPath = Path()
        for (j in 0 until n) {
            val angle = startAngle + j * angleStep
            val r = radius * dataValues[j]
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            if (j == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        canvas.drawPath(dataPath, fillPaint)
        canvas.drawPath(dataPath, strokePaint)

        // 5. 최근 5개 꼭짓점 점
        for (j in 0 until n) {
            val angle = startAngle + j * angleStep
            val r = radius * dataValues[j]
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            canvas.drawCircle(x, y, 6f, dotPaint)
        }

        // 6. 개수 라벨
        if (rawCounts.any { it > 0 }) {
            for (j in 0 until n) {
                val angle = startAngle + j * angleStep
                val r = (radius * dataValues[j] - 32f).coerceAtLeast(16f)
                val x = (cx + r * cos(angle)).toFloat()
                val y = (cy + r * sin(angle)).toFloat()
                val text = "${rawCounts[j]}"
                val bounds = Rect()
                countPaint.getTextBounds(text, 0, text.length, bounds)
                canvas.drawText(text, x, y + bounds.height() / 2f, countPaint)
            }
        }

        // 7. 카테고리 라벨
        for (j in 0 until n) {
            val angle = startAngle + j * angleStep
            val x = (cx + labelRadius * cos(angle)).toFloat()
            val y = (cy + labelRadius * sin(angle)).toFloat()
            val bounds = Rect()
            labelPaint.getTextBounds(labels[j], 0, labels[j].length, bounds)
            canvas.drawText(labels[j], x, y + bounds.height() / 2f, labelPaint)
        }
    }
}
