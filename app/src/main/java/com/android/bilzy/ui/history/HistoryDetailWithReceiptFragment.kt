package com.android.bilzy.ui.history

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.android.bilzy.ui.common.loading
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryDetailWithReceiptBinding
import com.android.bilzy.domain.model.MemberRoundAmount
import com.android.bilzy.domain.model.Receipt
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.ui.room.payerLine
import com.android.bilzy.util.ImageCompressor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat

/**
 * 정산 상세 + 첨부 영수증 화면. HistoryDetailViewModel(GET /settlements/{id})로 실데이터를 받아
 * 요약·참여자·영수증 이미지를 렌더한다. 정산방이 영수증 이미지를 가진 경우에만 영수증 섹션을 보여준다.
 * 요약카드/참여자카드는 Figma 실측 기준으로 SettlementResultFragment/HistoryDetailFragment와 동일 구조.
 */
@AndroidEntryPoint
class HistoryDetailWithReceiptFragment : Fragment() {

    private var _binding: FragmentHistoryDetailWithReceiptBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryDetailViewModel by viewModels()
    private val nf = NumberFormat.getInstance()

    private var pendingDownloadUrl: String? = null

    /** 완료된 정산방에 순수 기록용으로 영수증 사진만 추가(OCR·금액 계산 없음). */
    private val pickReceiptPhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(requireContext(), "이미지를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val mime = resolver.getType(uri) ?: "image/jpeg"
            uploadReceiptPhoto(bytes, mime)
        }

    private fun uploadReceiptPhoto(bytes: ByteArray, mime: String) {
        binding.btnAddReceipt.isEnabled = false
        loading.show()
        viewLifecycleOwner.lifecycleScope.launch {
            val compressed = ImageCompressor.compress(bytes, mime)
            val ok = viewModel.attachPhoto(compressed.bytes, compressed.mime)
            loading.hide()
            if (isAdded && _binding != null) {
                binding.btnAddReceipt.isEnabled = true
                if (!ok) {
                    Toast.makeText(requireContext(), "영수증 추가에 실패했어요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pendingDownloadUrl?.let { saveToGallery(it) }
            else Toast.makeText(requireContext(), "저장 권한이 필요해요", Toast.LENGTH_SHORT).show()
            pendingDownloadUrl = null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryDetailWithReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnAddReceipt.setOnClickListener { pickReceiptPhoto.launch("image/*") }

        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_home)
        }
        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_scanHub)
        }
        binding.navHistory.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_historyList)
        }
        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_myPage)
        }

        observeSettlement()
        observeMyUserId()
        arguments?.getString("settlementId")?.let { viewModel.load(it) }
    }

    private fun observeSettlement() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settlement.collect { settlement ->
                    loading.set(settlement == null)
                    settlement ?: return@collect
                    render(settlement)
                }
            }
        }
    }

    /** 내 id를 뒤늦게 알게 되면(프로필 캐시가 없던 경우) 요약 줄을 다시 그린다. */
    private fun observeMyUserId() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadFailed.collect {
                    loading.hide()
                    Toast.makeText(requireContext(), "불러오지 못했어요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.myUserId.collect { viewModel.settlement.value?.let { s -> renderPayerLine(s) } }
            }
        }
    }

    private fun render(s: Settlement) {
        binding.tvTitle.text = s.title.ifBlank { "정산" }
        val date = formatDate(s.createdAt)
        binding.tvSettlementName.text = s.title.ifBlank { "정산" } + (if (date.isNotBlank()) " · $date" else "")
        binding.tvTotalAmount.text = bigAmountSpan(s.totalAmount)
        renderRounds(s)
        renderPayerLine(s)

        val container = binding.personsContainer
        container.removeAllViews()
        roundParticipants = s.receipts.associate { it.round to s.roundParticipantCount(it.round) }
        s.members.forEach { m ->
            container.addView(personCard(m.nickname, m.amount, m.rounds, s.receipts))
        }

        renderReceipt(s)
    }

    /** 라운드(영수증)별 "N차 · 참여인원" 칩 + 합계. 라운드 사이 세로 구분선 포함. */
    private fun renderRounds(settlement: Settlement) {
        val container = binding.roundsContainer
        container.removeAllViews()
        val receipts = settlement.receipts.sortedBy { it.round }
        binding.roundsDivider.visibility = if (receipts.isEmpty()) View.GONE else View.VISIBLE
        receipts.forEachIndexed { index, receipt ->
            if (index > 0) {
                container.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(1), dp(48)).apply {
                        marginStart = dp(4); marginEnd = dp(4)
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    setBackgroundColor(Color.parseColor("#33FFFFFF"))
                })
            }
            val participants = settlement.members.count { m -> m.rounds.any { it.round == receipt.round } }
            container.addView(roundColumn(receipt.round, participants, receipt.totalAmount))
        }
    }

    private fun roundColumn(round: Int, participants: Int, amount: Long): View {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(ctx).apply {
            text = "${round}차 · ${participants}명"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setBackgroundResource(R.drawable.bg_round_chip)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })
        col.addView(TextView(ctx).apply {
            text = "${nf.format(amount)}원"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })
        return col
    }

    /** "{결제자} 전액 결제 · 받을 금액/내가 낼 금액 {금액}". 결제자를 찾을 수 없으면 숨김. */
    private fun renderPayerLine(settlement: Settlement) {
        // 결제자가 보면 "받을 금액", 참여자가 보면 "내가 낼 금액"(ui/room/PayerLine.kt)
        val line = payerLine(settlement, settlement.totalAmount, viewModel.myUserId.value)
        if (line == null) {
            binding.payerRow.visibility = View.GONE
            return
        }
        binding.payerRow.visibility = View.VISIBLE
        binding.tvPayerPrefix.text = line.first
        binding.tvPayerAmount.text = "${nf.format(line.second)}원"
    }

    /** 차수별 참여 인원. 제외 항목 칩에 품목 전체 금액이 아니라 내 몫에서 실제로 빠진 금액(품목÷인원)을 보여주는 데 쓴다. */
    private var roundParticipants: Map<Int, Int> = emptyMap()

    private fun personCard(
        name: String,
        amount: Long,
        roundAmounts: List<MemberRoundAmount>,
        receipts: List<Receipt>
    ): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_participant_card)
            setPadding(dp(17), dp(22), dp(16), dp(22))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        val row = RelativeLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        row.addView(TextView(ctx).apply {
            text = name
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { addRule(RelativeLayout.ALIGN_PARENT_START) }
        })
        row.addView(TextView(ctx).apply {
            text = "${nf.format(amount)}원"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        })
        card.addView(row)

        val roundsByNumber = roundAmounts.associateBy { it.round }
        data class RoundRow(val label: String, val amount: Long, val tags: List<String>)
        val rows: List<RoundRow> = if (receipts.isNotEmpty()) {
            receipts.sortedBy { it.round }.map { receipt ->
                val ra = roundsByNumber[receipt.round]
                if (ra != null) {
                    val itemTags = ra.excludedItemNames.mapNotNull { itemName ->
                        receipt.items.find { it.name == itemName }
                            ?.let { "$itemName -${nf.format(it.total / (roundParticipants[receipt.round] ?: 1).coerceAtLeast(1))}원" }
                    }
                    // 안 먹은 메뉴가 있으면 그것만("맥주 -5,000원"), 없으면 먹은 메뉴 이름만("피자", "맥주") 칩으로 보여준다.
                    // 예전엔 AI가 쓴 계산 설명 문장("피자(20000원/2명) 10000원 + …")이 통째로 들어갔다.
                    val tags = itemTags.ifEmpty {
                        receipt.items.map { it.name }.filter(String::isNotBlank).distinct()
                            .ifEmpty { listOf("1/N 정산") }
                    }
                    RoundRow("${receipt.round}차", ra.amount, tags)
                } else {
                    RoundRow("${receipt.round}차", 0L, listOf("미참여"))
                }
            }
        } else if (roundAmounts.isNotEmpty()) {
            roundAmounts.map {
                RoundRow("${it.round}차", it.amount, listOf(it.reason?.takeIf(String::isNotBlank) ?: "1/N 정산"))
            }
        } else {
            listOf(RoundRow("1차", amount, listOf("1/N 정산")))
        }
        rows.forEachIndexed { index, r ->
            if (index > 0) {
                card.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
                    ).apply { topMargin = dp(18) }
                    setBackgroundColor(Color.parseColor("#33FFFFFF"))
                })
            }
            card.addView(roundTagRow(r.label, r.amount, r.tags))
        }
        return card
    }

    /** 라운드별 "N차 - 금액" 텍스트 + 태그 칩들(제외 항목 개수만큼, 줄바꿈 가능). */
    private fun roundTagRow(round: String, amount: Long, tags: List<String>): View {
        val ctx = requireContext()
        val block = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
        }
        block.addView(TextView(ctx).apply {
            text = "$round - ${nf.format(amount)}원"
            setTextColor(Color.parseColor("#AAB2FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
        block.addView(
            com.google.android.material.chip.ChipGroup(ctx).apply {
                isSingleLine = false
                chipSpacingHorizontal = dp(8)
                chipSpacingVertical = dp(6)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                tags.forEach { tag ->
                    addView(TextView(ctx).apply {
                        text = tag
                        setTextColor(Color.parseColor("#67F874"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setBackgroundResource(R.drawable.bg_chip_green_outline)
                        setPadding(dp(12), dp(4), dp(12), dp(4))
                    })
                }
            }
        )
        return block
    }

    /** Figma 실측: 총액 숫자는 34sp, "원"은 22sp로 크기가 다르다. */
    private fun bigAmountSpan(amount: Long): android.text.SpannableString {
        val number = nf.format(amount)
        val full = "${number}원"
        val px22sp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 22f, resources.displayMetrics
        ).toInt()
        return android.text.SpannableString(full).apply {
            setSpan(
                android.text.style.AbsoluteSizeSpan(px22sp, false),
                number.length, full.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /** 라운드별 실제 영수증(스캔된 것) + 순수 첨부 사진(라운드·금액과 무관)을 모두 카드로 그린다. */
    private fun renderReceipt(s: Settlement) {
        val withImage = s.receipts.filter { !it.receiptImageUrl.isNullOrBlank() }.sortedBy { it.round }
        val extraPhotos = s.extraPhotos
        val hasReceipt = withImage.isNotEmpty() || extraPhotos.isNotEmpty()

        binding.receiptSectionHeader.isVisible = hasReceipt
        binding.receiptsContainer.isVisible = hasReceipt
        binding.receiptsContainer.removeAllViews()
        if (!hasReceipt) return

        binding.tvReceiptCount.text = "${withImage.size + extraPhotos.size}장"
        withImage.forEach { receipt ->
            binding.receiptsContainer.addView(receiptCard(receipt))
        }
        extraPhotos.forEach { photo ->
            binding.receiptsContainer.addView(extraPhotoCard(photo.imageUrl))
        }
    }

    private fun receiptCard(receipt: Receipt): View {
        val url = receipt.receiptImageUrl!!
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_attached_receipt)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            isClickable = true
            isFocusable = true
            setOnClickListener { showFullScreenImage(url) }
        }
        val thumb = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { marginEnd = dp(14) }
            setBackgroundResource(R.drawable.bg_receipt_thumb)
            scaleType = ImageView.ScaleType.CENTER_CROP
            load(url) {
                crossfade(true)
                error(R.drawable.bg_receipt_thumb)
                placeholder(R.drawable.bg_receipt_thumb)
            }
        }
        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(ctx).apply {
            text = "${receipt.round}차" + (receipt.storeName?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        })
        texts.addView(TextView(ctx).apply {
            text = "${nf.format(receipt.totalAmount)}원"
            setTextColor(Color.parseColor("#80FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        })
        val more = TextView(ctx).apply {
            text = "⋯"
            setTextColor(Color.parseColor("#80FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setPadding(dp(8), 0, dp(4), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { showReceiptOptions(it, url) }
        }
        card.addView(thumb)
        card.addView(texts)
        card.addView(more)
        return card
    }

    /** 라운드/금액 없이 순수 기록용으로 첨부된 사진 카드. */
    private fun extraPhotoCard(url: String): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_attached_receipt)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            isClickable = true
            isFocusable = true
            setOnClickListener { showFullScreenImage(url) }
        }
        val thumb = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { marginEnd = dp(14) }
            setBackgroundResource(R.drawable.bg_receipt_thumb)
            scaleType = ImageView.ScaleType.CENTER_CROP
            load(url) {
                crossfade(true)
                error(R.drawable.bg_receipt_thumb)
                placeholder(R.drawable.bg_receipt_thumb)
            }
        }
        val label = TextView(ctx).apply {
            text = "영수증 사진"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val more = TextView(ctx).apply {
            text = "⋯"
            setTextColor(Color.parseColor("#80FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setPadding(dp(8), 0, dp(4), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { showReceiptOptions(it, url) }
        }
        card.addView(thumb)
        card.addView(label)
        card.addView(more)
        return card
    }

    private fun showFullScreenImage(url: String) {
        val dialog = Dialog(requireContext())
        val iv = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            load(url) { crossfade(true) }
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(iv)
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
    }

    private fun showReceiptOptions(anchor: View, url: String) {
        PopupMenu(ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_Bilzy_PopupMenu), anchor).apply {
            menu.add(0, 0, 0, "크게 보기")
            menu.add(0, 1, 1, "이미지 저장")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> showFullScreenImage(url)
                    1 -> downloadAndSave(url)
                }
                true
            }
            show()
        }
    }

    private fun downloadAndSave(url: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownloadUrl = url
            requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        saveToGallery(url)
    }

    private fun saveToGallery(url: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { java.net.URL(url).readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: run {
                        Toast.makeText(requireContext(), "이미지 저장에 실패했어요", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "receipt_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/Bilzy")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: run {
                        Toast.makeText(requireContext(), "저장에 실패했어요", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "갤러리에 저장됐어요", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "저장에 실패했어요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** "2026-05-04T..." → "2026.05.04" */
    private fun formatDate(iso: String?): String {
        iso ?: return ""
        return iso.substringBefore('T').replace('-', '.')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
