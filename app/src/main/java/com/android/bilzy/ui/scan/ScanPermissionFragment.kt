package com.android.bilzy.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentScanPermissionBinding

/**
 * 영수증 스캔 진입 화면. "권한 허용하기"가 실제 카메라 런타임 권한을 요청한다.
 * 이미 허용돼 있으면 바로 카메라로, 영구 거부 상태면 앱 설정으로 안내한다.
 */
class ScanPermissionFragment : Fragment() {

    private var _binding: FragmentScanPermissionBinding? = null
    private val binding get() = _binding!!

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                goToCamera()
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // 영구 거부("다시 묻지 않음") → 설정으로 안내
                Toast.makeText(requireContext(), "설정 > 권한에서 카메라를 허용해주세요", Toast.LENGTH_LONG).show()
                openAppSettings()
            } else {
                Toast.makeText(requireContext(), "영수증 스캔에는 카메라 권한이 필요해요", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnCamera.setOnClickListener {
            if (hasCameraPermission()) {
                goToCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // 하단 네비게이션 (스캔은 현재 화면이라 별도 동작 없음)
        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_scanPermission_to_home)
        }
        binding.navHistory.setOnClickListener {
            findNavController().navigate(R.id.action_scanPermission_to_historyList)
        }
        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_scanPermission_to_myPage)
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun goToCamera() {
        findNavController().navigate(R.id.action_scanPermission_to_scanCamera)
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
