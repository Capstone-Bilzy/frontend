package com.android.favorie

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.databinding.FragmentAnalysisBinding
import com.android.favorie.network.RetrofitClient
import com.android.favorie.network.model.ItemResponse
import com.android.favorie.network.model.MonthlyReportResponse
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    data class CategoryCard(val name: String, val color: String, val iconRes: Int, val tags: List<String>)

    // 월 상태
    private var currentYear = 0
    private var currentMonth = 0  // 1–12
    private val reportCache = mutableMapOf<String, MonthlyReportResponse?>()
    private var isLoadingMonth = false
    private var prevMonthHasData = true

    // 최대 12개월 전까지만 탐색 허용
    private val minYear: Int
    private val minMonth: Int
    init {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -12)
        minYear = cal.get(java.util.Calendar.YEAR)
        minMonth = cal.get(java.util.Calendar.MONTH) + 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val now = Calendar.getInstance()
        currentYear = now.get(Calendar.YEAR)
        currentMonth = now.get(Calendar.MONTH) + 1

        showTotalTab()
        binding.btnTotalAnalysis.setOnClickListener { showTotalTab() }
        binding.btnMonthlyReport.setOnClickListener { showMonthlyTab() }

        // 종합 분석
        loadItemProgress()
        loadAndSetupCategoryCards()

        // 월별 리포트
        setupMonthNavigation()
    }

    // ===== 탭 전환 =====

    private fun showTotalTab() {
        binding.btnTotalAnalysis.setBackgroundResource(R.drawable.bg_tab_selected)
        binding.btnMonthlyReport.setBackgroundResource(R.drawable.bg_tab_unselected)
        binding.layoutTotalContent.visibility = View.VISIBLE
        binding.layoutMonthlyContent.visibility = View.GONE
    }

    private fun showMonthlyTab() {
        binding.btnMonthlyReport.setBackgroundResource(R.drawable.bg_tab_selected)
        binding.btnTotalAnalysis.setBackgroundResource(R.drawable.bg_tab_unselected)
        binding.layoutTotalContent.visibility = View.GONE
        binding.layoutMonthlyContent.visibility = View.VISIBLE
        loadAndShowCurrentMonth()
    }

    // ===== 아이템 진행도 바 =====

    private fun loadItemProgress() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getHome()
                if (!resp.isSuccessful) return@launch
                val home = resp.body() ?: return@launch
                val total = home.music + home.movie + home.book + home.space + home.fashion + home.vibe
                renderProgressDots(total)

                // 전체 아이템 병렬 조회 → ID 내림차순 정렬 → 최근 5개 vs 이전 5개
                val apiCategories = listOf("MUSIC", "MOOD", "PLACE", "FASHION", "BOOK", "MOVIE")
                val allItems = apiCategories.map { cat ->
                    async {
                        try { RetrofitClient.api.getItems(category = cat).body()?.items ?: emptyList() }
                        catch (_: Exception) { emptyList() }
                    }
                }.map { it.await() }.flatten().sortedByDescending { it.id }

                val recentGroup   = allItems.take(5)
                val previousGroup = allItems.drop(5).take(5)

                // labels 순서: MUSIC, VIBE(MOOD), SPACE(PLACE), FASHION, BOOK, MOVIE
                fun toCounts(items: List<ItemResponse>) = intArrayOf(
                    items.count { it.category == "MUSIC" },
                    items.count { it.category == "MOOD" },
                    items.count { it.category == "PLACE" },
                    items.count { it.category == "FASHION" },
                    items.count { it.category == "BOOK" },
                    items.count { it.category == "MOVIE" }
                )

                binding.radarChart.setCompareData(toCounts(recentGroup), toCounts(previousGroup))
            } catch (e: Exception) {
                Log.e("AnalysisFragment", "진행도 오류", e)
            }
        }
    }

    private fun renderProgressDots(total: Int) {
        val progress = if (total % 5 == 0 && total > 0) 5 else total % 5
        val remaining = 5 - progress

        binding.layoutProgressTrack.post {
            val trackWidth = binding.layoutProgressTrack.width
            val fillWidth = (trackWidth * progress / 5f).toInt()
            binding.viewProgressFill.layoutParams =
                binding.viewProgressFill.layoutParams.also { it.width = fillWidth }
            binding.viewProgressFill.requestLayout()
        }

        binding.tvProgressCount.text =
            if (remaining == 0) "총 ${total}개 · 최신화됨"
            else "${progress}/5"
    }

    // ===== 카테고리별 인기태그 TOP3 카드 + 나의 #키워드 =====

    private fun loadAndSetupCategoryCards() {
        val categoryMeta = listOf(
            Triple("MUSIC",   "#FFDD57", R.drawable.ic_music),
            Triple("MOVIE",   "#FF4B4B", R.drawable.ic_movie),
            Triple("BOOK",    "#3DBA6F", R.drawable.ic_book),
            Triple("PLACE",   "#FF8C00", R.drawable.ic_space),
            Triple("FASHION", "#4B9EFF", R.drawable.ic_fashion),
            Triple("MOOD",    "#BB3FBD", R.drawable.ic_vibe),
        )
        val displayNames = mapOf("PLACE" to "SPACE", "MOOD" to "VIBE")

        viewLifecycleOwner.lifecycleScope.launch {
            // 1. 카테고리별 아이템 목록 병렬 조회
            val itemsByCategory = categoryMeta.map { (apiCat, _, _) ->
                apiCat to async {
                    try { RetrofitClient.api.getItems(category = apiCat).body()?.items ?: emptyList() }
                    catch (_: Exception) { emptyList() }
                }
            }.map { (cat, deferred) -> cat to deferred.await() }.toMap()

            // 2. 태그 수집: 목록에 있으면 바로 사용, 없으면 detail 호출 (카테고리당 최대 5개)
            val tagsByCategory = categoryMeta.map { (apiCat, _, _) ->
                apiCat to async {
                    itemsByCategory[apiCat].orEmpty().take(5).map { item ->
                        async {
                            val listTags = (item.globalTags ?: emptyList()) + (item.localTags ?: emptyList())
                            if (listTags.isNotEmpty()) return@async listTags
                            try {
                                val d = RetrofitClient.api.getItemDetail(item.id).body()
                                (d?.globalTags ?: emptyList()) + (d?.localTags ?: emptyList())
                            } catch (_: Exception) { emptyList() }
                        }
                    }.flatMap { it.await() }
                }
            }.map { (cat, deferred) -> cat to deferred.await() }.toMap()

            // 3. 카테고리별 인기태그 TOP3 카드
            val cards = categoryMeta.map { (apiCat, color, icon) ->
                val topTags = tagsByCategory[apiCat].orEmpty()
                    .groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .take(3).map { "#${it.key}" }
                CategoryCard(displayNames[apiCat] ?: apiCat, color, icon, topTags)
            }

            // 4. 나의 #키워드: 전체 태그 빈도 TOP5
            val topKeywords = tagsByCategory.values.flatten()
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(5).map { "#${it.key}" }

            if (topKeywords.isNotEmpty()) {
                addTagsToFlexbox(
                    flexbox = binding.flexboxKeywords,
                    tags = topKeywords,
                    bgColor = "#2A2A2A",
                    textColor = "#FFFFFF",
                    strokeColor = "#666666"
                )
            }

            renderCategoryCards(cards)
        }
    }

    private fun renderCategoryCards(cards: List<CategoryCard>) {
        binding.layoutCategoryCards.removeAllViews()
        cards.forEach { card ->
            val cardView = layoutInflater.inflate(
                R.layout.item_category_card, binding.layoutCategoryCards, false
            )
            val ivIcon   = cardView.findViewById<ImageView>(R.id.iv_card_icon)
            val tvName   = cardView.findViewById<TextView>(R.id.tv_card_category_name)
            val flexTags = cardView.findViewById<FlexboxLayout>(R.id.flexbox_card_tags)

            val cardColor = Color.parseColor(card.color)
            ivIcon.setImageResource(card.iconRes)
            ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(cardColor)
            tvName.text = card.name
            tvName.setTextColor(cardColor)
            (cardView.background.mutate() as GradientDrawable).setStroke(4, cardColor)

            card.tags.forEach { tag ->
                val tagTv = TextView(requireContext()).apply {
                    text = tag; textSize = 13f
                    setTextColor(Color.WHITE)
                    setPadding(30, 14, 30, 14)
                    layoutParams = FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 14, 0) }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 60f; setColor(cardColor)
                    }
                }
                flexTags.addView(tagTv)
            }
            binding.layoutCategoryCards.addView(cardView)
        }
    }

    // ===== 월별 리포트: GET /reports/monthly =====

    private fun setupMonthNavigation() {
        updateMonthLabel()
        loadAndShowCurrentMonth()

        binding.btnPrevMonth.setOnClickListener {
            if (!isLoadingMonth && prevMonthHasData) navigateMonth(-1)
        }
        binding.btnNextMonth.setOnClickListener {
            if (!isLoadingMonth) navigateMonth(1)
        }
    }

    private fun navigateMonth(delta: Int) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth - 1, 1)
        cal.add(Calendar.MONTH, delta)

        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH) + 1

        val now = Calendar.getInstance()
        val nowYear = now.get(Calendar.YEAR)
        val nowMonth = now.get(Calendar.MONTH) + 1
        if (newYear > nowYear || (newYear == nowYear && newMonth > nowMonth)) return
        if (newYear < minYear || (newYear == minYear && newMonth < minMonth)) return

        currentYear = newYear
        currentMonth = newMonth
        updateMonthLabel()
        loadAndShowCurrentMonth()
    }

    private fun updateMonthLabel() {
        binding.tvCurrentMonth.text = monthAbbr(currentYear, currentMonth)

        val now = Calendar.getInstance()
        val isNow = currentYear == now.get(Calendar.YEAR) &&
                    currentMonth == now.get(Calendar.MONTH) + 1
        val isMin = currentYear < minYear || (currentYear == minYear && currentMonth <= minMonth)
        binding.btnNextMonth.alpha = if (isNow) 0.3f else 1f
        binding.btnPrevMonth.alpha = if (isMin) 0.3f else 1f
    }

    private fun loadAndShowCurrentMonth() {
        if (isLoadingMonth) return
        isLoadingMonth = true
        binding.btnPrevMonth.alpha = 0.3f
        binding.btnNextMonth.alpha = 0.3f

        val year = currentYear
        val month = currentMonth

        viewLifecycleOwner.lifecycleScope.launch {
            val currYM  = toYMString(year, month)
            val prev1YM = shiftMonthString(year, month, -1)
            val prev2YM = shiftMonthString(year, month, -2)

            val currD   = async { loadMonthReport(currYM) }
            val prev1D  = async { loadMonthReport(prev1YM) }
            val prev2D  = async { loadMonthReport(prev2YM) }
            val countsD = async { loadMonthlyCounts(currYM) }

            val curr   = currD.await()
            val prev1  = prev1D.await()
            val prev2  = prev2D.await()
            val counts = countsD.await()

            renderMonthlyReport(year, month, curr, prev1, prev2)
            if (curr != null) renderMonthlyBars(counts)

            if (curr != null) {
                binding.tvRecommendMonthLabel.text = "${year}년 ${month}월 취향에 딱 맞는 아이템"
                curr.recommendedItemId?.let { loadRecommendItem(it) }
            }

            prevMonthHasData = prev1 != null
            isLoadingMonth = false
            updateMonthLabel()
            if (!prevMonthHasData) binding.btnPrevMonth.alpha = 0.3f
        }
    }

    private suspend fun loadMonthReport(yearMonth: String): MonthlyReportResponse? {
        if (reportCache.containsKey(yearMonth)) return reportCache[yearMonth]
        return try {
            val resp = RetrofitClient.api.getMonthlyReport(yearMonth)
            val result = if (resp.isSuccessful) resp.body() else null
            reportCache[yearMonth] = result
            result
        } catch (e: Exception) {
            Log.e("AnalysisFragment", "월별 리포트 오류 $yearMonth", e)
            reportCache[yearMonth] = null
            null
        }
    }

    private fun renderMonthlyReport(
        year: Int, month: Int,
        curr: MonthlyReportResponse?,
        prev1: MonthlyReportResponse?,
        prev2: MonthlyReportResponse?
    ) {
        if (curr == null) {
            binding.tvMonthlyHeadline.visibility = View.VISIBLE
            binding.tvMonthlyHeadline.textSize = 15f
            binding.tvMonthlyHeadline.text = "이번 달 리포트는 다음 달에 공개됩니다"
            binding.cardMatrix.visibility = View.GONE
            binding.layoutMonthlyBarsCard.visibility = View.GONE
            binding.cardJourney.visibility = View.GONE
            binding.cardRecommendItem.visibility = View.GONE
            return
        }

        binding.tvMonthlyHeadline.visibility = View.VISIBLE
        binding.tvMonthlyHeadline.textSize = 22f
        binding.tvMonthlyHeadline.text = curr.summary ?: ""
        binding.cardMatrix.visibility = View.VISIBLE
        binding.cardJourney.visibility = View.VISIBLE
        binding.cardRecommendItem.visibility = View.VISIBLE

        // 매트릭스 dot 위치 + 태그
        positionMatrixDot(curr.diversityScore, curr.rarityScore)
        val badgeName = badgeLabel(curr.badge)
        binding.flexboxMatrixTags.removeAllViews()
        listOf(
            badgeName                                    to "#BB3FBD",
            "다양성 ${curr.diversityScore.toInt()}%"    to "#4B9EFF",
            "희귀도 ${curr.rarityScore.toInt()}%"       to "#FF8C00"
        ).forEach { (label, color) ->
            addSingleTag(binding.flexboxMatrixTags, label, color)
        }

        // 여정 탭
        updateJourneyTabs(year, month, curr, prev1, prev2)
    }

    private fun positionMatrixDot(diversity: Double, rarity: Double) {
        binding.gridMatrix.post {
            val w = binding.gridMatrix.width.toFloat()
            val h = binding.gridMatrix.height.toFloat()
            if (w <= 0 || h <= 0) return@post

            val dotPx = (12 * resources.displayMetrics.density).toInt()
            val x = ((w - dotPx) * (diversity / 100.0)).toInt().coerceIn(0, (w - dotPx).toInt())
            val y = ((h - dotPx) * (1.0 - rarity / 100.0)).toInt().coerceIn(0, (h - dotPx).toInt())

            val params = binding.viewMatrixDot.layoutParams as ConstraintLayout.LayoutParams
            params.marginStart = x
            params.topMargin = y
            binding.viewMatrixDot.layoutParams = params
        }
    }

    private fun updateJourneyTabs(
        year: Int, month: Int,
        curr: MonthlyReportResponse?,
        prev1: MonthlyReportResponse?,
        prev2: MonthlyReportResponse?
    ) {
        val prev1Cal = Calendar.getInstance().also { it.set(year, month - 1, 1); it.add(Calendar.MONTH, -1) }
        val prev2Cal = Calendar.getInstance().also { it.set(year, month - 1, 1); it.add(Calendar.MONTH, -2) }

        val tabLeft  = binding.btnJourneyApr
        val tabMid   = binding.btnJourneyMay
        val tabRight = binding.btnJourneyJun

        tabLeft.text  = monthAbbr(prev2Cal.get(Calendar.YEAR), prev2Cal.get(Calendar.MONTH) + 1)
        tabMid.text   = monthAbbr(prev1Cal.get(Calendar.YEAR), prev1Cal.get(Calendar.MONTH) + 1)
        tabRight.text = monthAbbr(year, month)

        val badgeLeft  = prev2?.let { badgeLabel(it.badge) } ?: "-"
        val badgeMid   = prev1?.let { badgeLabel(it.badge) } ?: "-"
        val badgeRight = curr?.let  { badgeLabel(it.badge) } ?: "-"

        fun selectTab(selected: TextView, badge: String) {
            listOf(tabLeft, tabMid, tabRight).forEach { tab ->
                tab.setBackgroundResource(
                    if (tab == selected) R.drawable.bg_journey_tab_selected
                    else R.drawable.bg_journey_tab_unselected
                )
                tab.setTextColor(
                    if (tab == selected) Color.WHITE else Color.parseColor("#888888")
                )
            }
            binding.tvJourneyBadge.text = badge
        }

        selectTab(tabRight, badgeRight)
        tabLeft.setOnClickListener  { selectTab(tabLeft,  badgeLeft) }
        tabMid.setOnClickListener   { selectTab(tabMid,   badgeMid) }
        tabRight.setOnClickListener { selectTab(tabRight, badgeRight) }
    }

    private fun loadRecommendItem(itemId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getItemDetail(itemId)
                val item = resp.body() ?: return@launch
                binding.tvRecommendTitle.text = item.title
                binding.tvRecommendSub.text = item.category
                if (!item.imageUrl.isNullOrBlank()) {
                    Glide.with(this@AnalysisFragment)
                        .load(item.imageUrl)
                        .placeholder(R.drawable.img_sample)
                        .centerCrop()
                        .into(binding.ivRecommendThumb)
                }

                val mainTab = when (item.category.uppercase()) {
                    "PLACE" -> "SPACE"
                    "MOOD"  -> "VIBE"
                    else    -> item.category.uppercase()
                }
                binding.cardRecommendItem.setOnClickListener {
                    val fragment = MusicDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString("mainTab", mainTab)
                            putString("category", item.category)
                            putLong("targetItemId", itemId)
                        }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            } catch (e: Exception) {
                Log.e("AnalysisFragment", "추천 아이템 오류", e)
            }
        }
    }

    // ===== 월별 카테고리 바 =====

    private suspend fun loadMonthlyCounts(yearMonth: String): IntArray = coroutineScope {
        // labels 순서: MUSIC, VIBE(MOOD), SPACE(PLACE), FASHION, BOOK, MOVIE
        val apiCats = listOf("MUSIC", "MOOD", "PLACE", "FASHION", "BOOK", "MOVIE")
        apiCats.map { cat ->
            async {
                try {
                    RetrofitClient.api.getItems(category = cat).body()?.items
                        ?.count { it.createdAt?.startsWith(yearMonth) == true } ?: 0
                } catch (_: Exception) { 0 }
            }
        }.map { it.await() }.toIntArray()
    }

    private fun renderMonthlyBars(counts: IntArray) {
        val labels = listOf("MUSIC", "VIBE", "SPACE", "FASHION", "BOOK", "MOVIE")
        val colors = listOf("#FFDD57", "#BB3FBD", "#FF8C00", "#4B9EFF", "#3DBA6F", "#FF4B4B")
        val dp = resources.displayMetrics.density

        binding.layoutMonthlyBars.removeAllViews()

        val max = counts.maxOrNull()?.takeIf { it > 0 } ?: run {
            binding.layoutMonthlyBarsCard.visibility = android.view.View.GONE
            return
        }
        binding.layoutMonthlyBarsCard.visibility = android.view.View.VISIBLE

        labels.forEachIndexed { i, label ->
            val count = counts[i]
            val filled = ((count.toFloat() / max) * 100).toInt().coerceAtLeast(if (count > 0) 2 else 0)
            val empty  = 100 - filled

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = (10 * dp).toInt() }
            }

            // 카테고리 이름
            row.addView(TextView(requireContext()).apply {
                text = label
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(colors[i]))
                layoutParams = LinearLayout.LayoutParams((64 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            })

            // 바 트랙 (내부 LinearLayout으로 채움 비율 표현)
            val track = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, (18 * dp).toInt(), 1f)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 9 * dp
                    setColor(Color.parseColor("#222222"))
                }
                clipToOutline = true
            }
            if (filled > 0) {
                track.addView(android.view.View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, filled.toFloat())
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 9 * dp
                        setColor(Color.parseColor(colors[i]))
                    }
                })
            }
            if (empty > 0) {
                track.addView(android.view.View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, empty.toFloat())
                })
            }
            row.addView(track)

            // 아이템 수
            row.addView(TextView(requireContext()).apply {
                text = count.toString()
                textSize = 11f
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    (28 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginStart = (8 * dp).toInt() }
            })

            binding.layoutMonthlyBars.addView(row)
        }
    }

    // ===== 유틸 =====

    private fun toYMString(year: Int, month: Int) = String.format("%04d-%02d", year, month)

    private fun shiftMonthString(year: Int, month: Int, delta: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.add(Calendar.MONTH, delta)
        return toYMString(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun monthAbbr(year: Int, month: Int): String {
        val cal = Calendar.getInstance().also { it.set(year, month - 1, 1) }
        return SimpleDateFormat("MMM", Locale.ENGLISH).format(cal.time).uppercase()
    }

    private fun badgeLabel(badge: String) = when (badge.uppercase()) {
        "EXPLORER"     -> "탐험가"
        "TRENDSETTER"  -> "트렌드세터"
        "CONNOISSEUR"  -> "취향장인"
        "COLLECTOR"    -> "덕후"
        else           -> badge
    }

    private fun addSingleTag(flexbox: FlexboxLayout, label: String, colorHex: String) {
        val tv = TextView(requireContext()).apply {
            text = label; textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(28, 14, 28, 14)
            layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 10, 8) }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
                setColor(Color.parseColor(colorHex))
            }
        }
        flexbox.addView(tv)
    }

    private fun addTagsToFlexbox(
        flexbox: FlexboxLayout,
        tags: List<String>,
        bgColor: String,
        textColor: String,
        strokeColor: String = ""
    ) {
        flexbox.removeAllViews()
        tags.forEach { tag ->
            val tv = TextView(requireContext()).apply {
                text = tag; textSize = 14f
                setTextColor(Color.parseColor(textColor))
                setPadding(36, 16, 36, 16)
                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 12, 12) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 60f
                    setColor(Color.parseColor(bgColor))
                    if (strokeColor.isNotEmpty()) setStroke(2, Color.parseColor(strokeColor))
                }
            }
            flexbox.addView(tv)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
