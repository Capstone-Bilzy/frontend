package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentOcrResultBinding
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

@AndroidEntryPoint
class OcrResultFragment : Fragment() {

    private var _binding: FragmentOcrResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private lateinit var adapter: OcrItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOcrResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OcrItemAdapter(onDelete = { index -> viewModel.removeItem(index) })
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        if (viewModel.settlementTitle.isNotEmpty()) {
            binding.etGroupName.setText(viewModel.settlementTitle)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_ocrResult_to_home)
        }
        binding.btnAddItem.setOnClickListener { showAddItemDialog() }
        binding.btnStart.setOnClickListener {
            val title = binding.etGroupName.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "모임 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.confirm(title)
        }

        observeItems()
        observeConfirm()
    }

    private fun observeItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submit(items)
                    binding.tvTotal.text = won(items.sumOf { it.subtotal })
                }
            }
        }
    }

    private fun observeConfirm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.confirmState.collect { state ->
                    when (state) {
                        is ScanFlowViewModel.ConfirmState.Loading -> binding.btnStart.isEnabled = false
                        is ScanFlowViewModel.ConfirmState.Success -> {
                            binding.btnStart.isEnabled = true
                            viewModel.consumeConfirmState()
                            findNavController().navigate(R.id.action_ocrResult_to_peopleCount)
                        }
                        is ScanFlowViewModel.ConfirmState.Error -> {
                            binding.btnStart.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeConfirmState()
                        }
                        is ScanFlowViewModel.ConfirmState.Idle -> binding.btnStart.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showAddItemDialog() {
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val nameInput = EditText(ctx).apply {
            hint = "항목명"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val priceInput = EditText(ctx).apply {
            hint = "가격(원)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(nameInput)
            addView(priceInput)
        }
        AlertDialog.Builder(ctx)
            .setTitle("항목 추가")
            .setView(container)
            .setPositiveButton("추가") { _, _ ->
                val name = nameInput.text.toString().trim()
                val price = priceInput.text.toString().trim().toLongOrNull() ?: 0L
                if (name.isEmpty() || price <= 0L) {
                    Toast.makeText(ctx, "항목명과 가격을 입력해주세요", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addItem(name, price, 1)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun won(value: Long) = NumberFormat.getInstance().format(value) + "원"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
