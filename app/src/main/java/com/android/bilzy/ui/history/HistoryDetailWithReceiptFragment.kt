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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.bilzy.data.demo.DemoData
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryDetailWithReceiptBinding
import com.android.bilzy.domain.model.Settlement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat

/**
 * 정산 상세 + 첨부 영수증 화면. HistoryDetailViewModel(GET /settlements/{id})로 실데이터를 받아
 * 요약·참여자·영수증 이미지를 렌더한다. 정산방이 영수증 이미지를 가진 경우에만 영수증 섹션을 보여준다.
 */
@AndroidEntryPoint
class HistoryDetailWithReceiptFragment : Fragment() {

    private var _binding: FragmentHistoryDetailWithReceiptBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryDetailViewModel by viewModels()
    private val nf = NumberFormat.getInstance()

    private var pendingDownloadUrl: String? = null

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

        binding.rvParticipants.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

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
        arguments?.getString("settlementId")?.let { viewModel.load(it) }
    }

    private fun observeSettlement() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    render(settlement)
                }
            }
        }
    }

    private fun render(s: Settlement) {
        binding.tvTitle.text = s.title.ifBlank { "정산" }
        binding.tvSettlementName.text = s.title.ifBlank { "정산" }
        binding.tvTotalAmount.text = nf.format(s.totalAmount) + "원"
        binding.tvPeopleChip.text = "👥 ${s.members.size}명 참여"
        binding.tvDate.text = formatDate(s.createdAt)

        val demoItems = if (s.id == DemoData.DEMO_ID) DemoData.memberItems else emptyMap()
        val participants = s.members.map { m ->
            ParticipantItem(
                name = m.nickname,
                items = demoItems[m.id] ?: "",
                amount = nf.format(m.amount) + "원",
                adjustment = m.reason?.takeIf { it.isNotBlank() }
            )
        }
        binding.rvParticipants.adapter = HistoryParticipantAdapter(participants)

        renderAvatars(s.members.size)
        renderReceipt(s)
    }

    private fun renderAvatars(count: Int) {
        val row = binding.avatarRow
        row.removeAllViews()
        if (count == 0) return

        val maxVisible = 4
        val visible = minOf(count, maxVisible)
        val dp32 = dp(32)
        val dpNeg8 = dp(-8)

        repeat(visible) { i ->
            val isLast = i == visible - 1 && count <= maxVisible
            val circle = View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_avatar_circle)
                layoutParams = LinearLayout.LayoutParams(dp32, dp32).apply {
                    marginEnd = if (isLast) 0 else dpNeg8
                }
            }
            row.addView(circle)
        }

        if (count > maxVisible) {
            val extra = count - maxVisible
            val badge = FrameLayout(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_avatar_circle)
                layoutParams = LinearLayout.LayoutParams(dp32, dp32)
            }
            badge.addView(TextView(requireContext()).apply {
                text = "+$extra"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
            row.addView(badge)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /** 정산방이 영수증 이미지를 가졌을 때만 영수증 섹션을 보여주고 Coil로 로드. */
    private fun renderReceipt(s: Settlement) {
        val url = s.receiptImageUrl?.takeIf { it.isNotBlank() }
        val hasReceipt = url != null

        binding.receiptSectionHeader.isVisible = hasReceipt
        binding.cardAttachedReceipt.isVisible = hasReceipt

        if (hasReceipt) {
            binding.tvReceiptCount.text = "1장"
            binding.tvReceiptName.text = s.title.ifBlank { "영수증" }
            binding.tvReceiptMeta.text = nf.format(s.totalAmount) + "원 · " + formatDate(s.createdAt)
            binding.ivReceiptThumb.load(url) {
                crossfade(true)
                error(R.drawable.bg_receipt_thumb)
                placeholder(R.drawable.bg_receipt_thumb)
            }
            binding.cardAttachedReceipt.setOnClickListener { showFullScreenImage(url!!) }
            binding.tvReceiptMore.setOnClickListener { showReceiptOptions(it, url!!) }
        } else {
            binding.cardAttachedReceipt.setOnClickListener(null)
        }
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
        PopupMenu(requireContext(), anchor).apply {
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
