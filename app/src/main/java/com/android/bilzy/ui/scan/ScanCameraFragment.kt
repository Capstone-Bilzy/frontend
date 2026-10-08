package com.android.bilzy.ui.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentScanCameraBinding
import com.android.bilzy.util.ImageCompressor
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.edit
import kotlinx.coroutines.launch

/** 사진 접근 모달("Bilzy가 사진에 접근하도록 허용하시겠습니까?")을 한 번 통과했는지 로컬에 기억해서 다음부턴 건너뛴다. */
internal object PhotoAccessPrefs {
    private const val PREFS_NAME = "bilzy_prefs"
    private const val KEY_ALLOWED = "photo_access_allowed"

    fun isAllowed(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ALLOWED, false)

    fun setAllowed(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_ALLOWED, true) }
    }
}

@AndroidEntryPoint
class ScanCameraFragment : Fragment() {

    private var _binding: FragmentScanCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var imageCapture: ImageCapture? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(requireContext(), "카메라 권한이 필요해요. 갤러리에서 선택할 수 있어요", Toast.LENGTH_SHORT).show()
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(requireContext(), "이미지를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val mime = resolver.getType(uri) ?: "image/jpeg"
            proceedWith(bytes, mime)
        }

    /** 이미지를 압축한 뒤 ViewModel에 넘기고 인식 화면으로 이동한다. */
    private fun proceedWith(bytes: ByteArray, mime: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val compressed = ImageCompressor.compress(bytes, mime)
            viewModel.setPendingImage(compressed.bytes, compressed.mime)
            findNavController().navigate(R.id.recognizingFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvRoundBadge.text = "${viewModel.currentRound}차"

        binding.btnBack.setOnClickListener { goBack() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { goBack() }
        binding.btnShutter.setOnClickListener { capture() }
        binding.btnGallery.setOnClickListener {
            if (PhotoAccessPrefs.isAllowed(requireContext())) {
                pickImage.launch("image/*")
            } else {
                showPhotoAccessOverlay()
            }
        }
        binding.tabQr.setOnClickListener {
            findNavController().navigate(R.id.action_scanCamera_to_qrScan)
        }

        setupPhotoAccessOverlay()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    /** "사진 선택..."/"모든 사진에 접근 허용"/"허용 안 함" 클릭 처리. */
    private fun setupPhotoAccessOverlay() {
        binding.blurView.setupWith(binding.blurTarget)
            .setBlurRadius(16f)
        binding.blurView.setOverlayColor(0x66000000)

        // 프로토타입: "사진 선택..."/"모든 사진에 접근 허용" 둘 다 같은 동작(사진 선택기 열기).
        // 한 번 허용하면 다음부턴 이 오버레이를 건너뛰도록 기억해 둔다.
        binding.btnPhotoLibrary.setOnClickListener {
            PhotoAccessPrefs.setAllowed(requireContext())
            hidePhotoAccessOverlay()
            pickImage.launch("image/*")
        }
        binding.btnFile.setOnClickListener {
            PhotoAccessPrefs.setAllowed(requireContext())
            hidePhotoAccessOverlay()
            pickImage.launch("image/*")
        }
        binding.btnCancel.setOnClickListener {
            Toast.makeText(requireContext(), "사진 접근을 허용하지 않았어요", Toast.LENGTH_SHORT).show()
            hidePhotoAccessOverlay()
        }
    }

    private fun showPhotoAccessOverlay() {
        binding.photoAccessOverlay.visibility = View.VISIBLE
    }

    private fun hidePhotoAccessOverlay() {
        binding.photoAccessOverlay.visibility = View.GONE
    }

    /**
     * 뒤로가기: 추가 스캔 중이면 방금 확정한 차수의 결과 화면으로, 앞 차수를 다시 찍는 중이면 영수증 목록으로
     * 돌아간다(둘 다 백스택에서는 이미 빠져 있어 그냥 올라가면 홈이 나온다). 첫 스캔이면 원래대로 이전 화면.
     */
    private fun goBack() {
        val nav = findNavController()
        val popCamera = NavOptions.Builder().setPopUpTo(R.id.scanCameraFragment, true).build()
        when {
            viewModel.returnToConfirmedRound() -> nav.navigate(R.id.ocrResultFragment, null, popCamera)
            viewModel.isRescanning -> nav.navigate(R.id.receiptListFragment, null, popCamera)
            else -> nav.navigateUp()
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "카메라를 열 수 없어요", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun capture() {
        val capture = imageCapture ?: run {
            Toast.makeText(requireContext(), "카메라가 준비되지 않았어요", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnShutter.isEnabled = false
        capture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = image.toJpegBytes()
                    image.close()
                    binding.btnShutter.isEnabled = true
                    proceedWith(bytes, "image/jpeg")
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.btnShutter.isEnabled = true
                    Toast.makeText(requireContext(), "촬영에 실패했어요", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** ImageCapture가 준 JPEG ImageProxy → ByteArray */
private fun ImageProxy.toJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}
