package com.android.bilzy.ui.settlement

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentQrInviteBinding
import com.android.bilzy.ui.room.RoomViewModel
import com.android.bilzy.ui.scan.ScanFlowViewModel
import com.android.bilzy.util.QrGenerator
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class QrInviteFragment : Fragment() {

    private var _binding: FragmentQrInviteBinding? = null
    private val binding get() = _binding!!

    private val scanViewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    /** 표시 중인 QR 비트맵 (저장/공유에 재사용) */
    private var qrBitmap: Bitmap? = null
    private var joinLink: String? = null

    // Android 9 이하에서 갤러리 저장에 필요한 쓰기 권한 요청 런처
    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) saveQrToGallery()
            else toast("저장하려면 저장소 권한이 필요해요")
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrInviteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showQr()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSaveImage.setOnClickListener {
            if (qrBitmap == null) { toast("QR이 아직 준비되지 않았어요"); return@setOnClickListener }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                saveQrToGallery()
            }
        }

        binding.btnShareQr.setOnClickListener { shareQr() }

        binding.btnEnter.setOnClickListener {
            roomViewModel.setRoom(scanViewModel.settlementId)
            findNavController().navigate(R.id.action_qrInvite_to_enteringRoom)
        }
    }

    /** 정산방 딥링크(bilzy://join/{id})를 QR로 만들어 표시. QrScanFragment가 이 형식을 읽는다. */
    private fun showQr() {
        val title = scanViewModel.settlementTitle.ifBlank { "정산방" }
        binding.tvRoomTitle.text = title

        val id = scanViewModel.settlementId
        if (id.isNullOrBlank()) {
            binding.tvRoomCode.text = "정산방 생성 후 QR이 표시돼요"
            return
        }

        val link = "bilzy://join/$id"
        joinLink = link
        runCatching { QrGenerator.encode(link, size = 512) }
            .onSuccess { bitmap ->
                qrBitmap = bitmap
                binding.ivQr.setImageBitmap(bitmap)
                binding.ivQr.imageTintList = null   // 생성된 QR은 원본 색 그대로
                binding.ivQr.setPadding(0, 0, 0, 0)
                binding.tvRoomCode.text = "QR을 스캔해 정산방에 입장"
            }
            .onFailure {
                binding.tvRoomCode.text = "QR 생성에 실패했어요"
            }
    }

    /** QR 비트맵을 기기 갤러리(Pictures/Bilzy)에 PNG로 저장. */
    private fun saveQrToGallery() {
        val bitmap = qrBitmap ?: return
        val resolver = requireContext().contentResolver
        val name = "bilzy_qr_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Bilzy")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) { toast("저장에 실패했어요"); return }

        runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: error("출력 스트림을 열 수 없어요")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        }.onSuccess {
            toast("갤러리에 저장했어요")
        }.onFailure {
            resolver.delete(uri, null, null)
            toast("저장에 실패했어요")
        }
    }

    /** QR 비트맵을 캐시에 쓰고 FileProvider URI로 다른 앱에 공유. */
    private fun shareQr() {
        val bitmap = qrBitmap ?: run { toast("QR이 아직 준비되지 않았어요"); return }
        runCatching {
            val dir = File(requireContext().cacheDir, "qr").apply { mkdirs() }
            val file = File(dir, "qr_share.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                joinLink?.let { putExtra(Intent.EXTRA_TEXT, "Bilzy 정산방에 입장하세요\n$it") }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "QR코드 공유"))
        }.onFailure {
            toast("공유에 실패했어요")
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
