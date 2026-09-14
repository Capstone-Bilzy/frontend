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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentQrScanBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * QR 카메라 인식 화면. CameraX 프리뷰 + ML Kit 바코드 분석으로
 * `bilzy://join/{settlement_id}` 딥링크를 읽어 정산방에 참여한다.
 */
@AndroidEntryPoint
class QrScanFragment : Fragment() {

    private var _binding: FragmentQrScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QrScanViewModel by viewModels()
    private val roomViewModel: com.android.bilzy.ui.room.RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private lateinit var cameraExecutor: ExecutorService
    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    /** QR을 한 번 인식해 처리에 들어가면 이후 프레임은 무시. */
    private var handled = false

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(requireContext(), "설정 > 권한에서 카메라를 허용해주세요", Toast.LENGTH_LONG).show()
                openAppSettings()
            } else {
                Toast.makeText(requireContext(), "QR 인식에 카메라 권한이 필요해요", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            scanFromUri(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_qrScan_to_home)
        }
        binding.tabReceipt.setOnClickListener {
            findNavController().navigate(R.id.action_qrScan_to_scanCamera)
        }
        binding.btnGallery.setOnClickListener { pickImage.launch("image/*") }
        // QR은 자동 인식이라 셔터는 별도 동작 없이 안내만
        binding.btnShutter.setOnClickListener {
            Toast.makeText(requireContext(), "QR이 화면에 보이면 자동으로 인식돼요", Toast.LENGTH_SHORT).show()
        }

        observeJoin()

        if (hasCameraPermission()) startCamera()
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun observeJoin() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.joinState.collect { state ->
                    when (state) {
                        is QrScanViewModel.JoinState.Success -> {
                            roomViewModel.expectedCount = 0  // 게스트: 인원 게이팅 없음
                            roomViewModel.setRoom(state.settlementId)
                            viewModel.consumeState()
                            findNavController().navigate(R.id.action_qrScan_to_roundPick)
                        }
                        is QrScanViewModel.JoinState.Error -> {
                            handled = false // 재인식 허용
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun startCamera() {
        binding.placeholderHint.visibility = View.GONE
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ::analyze) }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "카메라를 열 수 없어요", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @ExperimentalGetImage
    private fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null || handled) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let { onQrDetected(it) }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun scanFromUri(uri: Uri) {
        val input = try {
            InputImage.fromFilePath(requireContext(), uri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "이미지를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
            return
        }
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstNotNullOfOrNull { it.rawValue }
                if (raw != null) onQrDetected(raw)
                else Toast.makeText(requireContext(), "QR 코드를 찾지 못했어요", Toast.LENGTH_SHORT).show()
            }
    }

    /** 인식한 QR 문자열에서 settlement_id·token을 추출해 참여를 시도. */
    private fun onQrDetected(raw: String) {
        val invite = com.android.bilzy.util.JoinLink.parse(raw)
        if (invite == null) {
            Toast.makeText(requireContext(), "Bilzy 정산방 QR이 아니에요", Toast.LENGTH_SHORT).show()
            return
        }
        if (handled) return
        handled = true
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.needsNicknamePrompt()) {
                findNavController().navigate(
                    R.id.action_qrScan_to_participantInput,
                    androidx.core.os.bundleOf(
                        "pendingSettlementId" to invite.settlementId,
                        "pendingToken" to invite.token
                    )
                )
            } else {
                viewModel.join(invite.settlementId, invite.token)
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
