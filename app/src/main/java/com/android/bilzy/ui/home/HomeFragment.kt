package com.android.bilzy.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.ui.common.loading
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHomeBinding
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val scanFlowViewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
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

        // 정산을 하다 말고 홈으로 나왔으면 그 스캔 흐름은 끝난 것 — 다음 스캔이 이전 정산방의
        // 다음 차수(4차 등)로 이어지지 않고 새 정산방 1차부터 시작하도록 상태를 비운다.
        scanFlowViewModel.reset()

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
                viewModel.loadFailed.collect {
                    loading.hide()
                    Toast.makeText(requireContext(), "불러오지 못했어요", Toast.LENGTH_SHORT).show()
                    // 보여줄 내역(캐시)도 없으면 "내역이 없어요" 문구는 숨긴다 — 없는 게 아니라 못 불러온 것.
                    if (viewModel.history.value == null) binding.tvHistoryEmpty.visibility = View.GONE
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.history.collect { list ->
                    loading.set(list == null)
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
