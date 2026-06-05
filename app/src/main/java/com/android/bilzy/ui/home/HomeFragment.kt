package com.android.bilzy.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var historyAdapter: HomeHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardPayer.setOnClickListener { startReceiptScan() }

        binding.cardParticipant.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qrScan)
        }

        binding.navScan.setOnClickListener { startReceiptScan() }

        binding.navHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historyList)
        }
        binding.btnSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historyList)
        }

        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_myPage)
        }

        setupHistory()
        observeHistory()
    }

    override fun onResume() {
        super.onResume()
        // 정산 완료 후 홈 복귀 시 최신 내역을 다시 불러온다.
        viewModel.loadHistory()
    }

    private fun setupHistory() {
        historyAdapter = HomeHistoryAdapter { item ->
            findNavController().navigate(
                R.id.action_home_to_historyDetail,
                bundleOf("settlementId" to item.settlementId)
            )
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = historyAdapter
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.history.collect { list ->
                    list ?: return@collect  // 로딩 중
                    historyAdapter.submit(list)
                    binding.rvHistory.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                    binding.tvHistoryEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    /**
     * 영수증 스캔 진입. 카메라 권한이 이미 있으면 권한 안내 화면을 건너뛰고
     * 바로 카메라로, 없으면 권한 안내(요청) 화면으로 보낸다.
     */
    private fun startReceiptScan() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val action = if (granted) R.id.action_home_to_scanCamera
        else R.id.action_home_to_scanPermission
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
