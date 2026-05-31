package com.android.bilzy.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentScanCameraBinding
import dagger.hilt.android.AndroidEntryPoint

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnShutter.setOnClickListener { capture() }
        binding.btnGallery.setOnClickListener { pickImage.launch("image/*") }
        binding.tabQr.setOnClickListener {
            findNavController().navigate(R.id.action_scanCamera_to_qrScan)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
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

    /** 이미지를 ViewModel에 넘기고 인식(업로드) 화면으로 이동 */
    private fun proceedWith(bytes: ByteArray, mime: String) {
        viewModel.setPendingImage(bytes, mime)
        findNavController().navigate(R.id.recognizingFragment)
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
