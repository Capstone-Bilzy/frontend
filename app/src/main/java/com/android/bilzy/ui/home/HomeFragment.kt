package com.android.bilzy.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

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

        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_myPage)
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
