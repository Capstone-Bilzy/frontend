package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentQrInviteBinding
import com.android.bilzy.ui.room.RoomViewModel
import com.android.bilzy.ui.scan.ScanFlowViewModel
import com.android.bilzy.util.QrGenerator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QrInviteFragment : Fragment() {

    private var _binding: FragmentQrInviteBinding? = null
    private val binding get() = _binding!!

    private val scanViewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

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
        runCatching { QrGenerator.encode(link, size = 512) }
            .onSuccess { bitmap ->
                binding.ivQr.setImageBitmap(bitmap)
                binding.ivQr.imageTintList = null   // 생성된 QR은 원본 색 그대로
                binding.ivQr.setPadding(0, 0, 0, 0)
                binding.tvRoomCode.text = "QR을 스캔해 정산방에 입장"
            }
            .onFailure {
                binding.tvRoomCode.text = "QR 생성에 실패했어요"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
