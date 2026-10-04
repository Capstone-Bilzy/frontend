package com.android.bilzy.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R

/**
 * 정산내역/마이페이지 등의 하단 "스캔" 탭이 향하는 경유지. 화면을 보여주지 않고 홈의 스캔 진입과 동일하게
 * 곧바로 카메라(권한이 없으면 권한 안내)로 보낸다.
 *
 * 예전엔 카메라처럼 생겼지만 실제 카메라는 없는 화면을 띄워서, 셔터를 한 번 더 눌러야 진짜 카메라가 떴다.
 * 스택에서 자신을 빼므로 카메라에서 뒤로가기 하면 원래 탭으로 돌아간다.
 */
class ScanHubFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = View(requireContext()).apply {
        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_dark))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        findNavController().navigate(
            if (granted) R.id.scanCameraFragment else R.id.scanPermissionFragment,
            null,
            NavOptions.Builder().setPopUpTo(R.id.scanHubFragment, true).build()
        )
    }
}
