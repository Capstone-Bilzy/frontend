package com.android.bilzy.ui.room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentJoinConfirmBinding
import com.android.bilzy.ui.scan.QrScanViewModel
import com.android.bilzy.util.JoinLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 초대 딥링크(`bilzy://join/{id}`)로 진입했을 때의 **입장 확인 게이트**.
 *
 * 보안상 외부 링크는 절대 자동으로 정산방에 가입시키지 않는다. 이 화면에서 사용자가
 * 명시적으로 "입장하기"를 눌러야만 join(POST)이 일어난다. settlement_id는 진입 시 한 번 더
 * UUID 형식을 검증한다(MainActivity에서 검증했더라도 방어적으로 재확인).
 */
@AndroidEntryPoint
class JoinConfirmFragment : Fragment() {

    private var _binding: FragmentJoinConfirmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QrScanViewModel by viewModels()
    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private val settlementId: String? by lazy { arguments?.getString(ARG_SETTLEMENT_ID) }
    private val token: String? by lazy { arguments?.getString(ARG_TOKEN) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 방어적 재검증 — 잘못된 링크면 입장 자체를 막는다.
        if (!JoinLink.isValidId(settlementId)) {
            Toast.makeText(requireContext(), "유효하지 않은 초대 링크예요", Toast.LENGTH_SHORT).show()
            goHome()
            return
        }

        observeJoin()

        binding.btnCancel.setOnClickListener { goHome() }
        binding.btnJoin.setOnClickListener {
            setLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                if (viewModel.needsNicknamePrompt()) {
                    findNavController().navigate(
                        R.id.action_joinConfirm_to_participantInput,
                        androidx.core.os.bundleOf(
                            "pendingSettlementId" to settlementId,
                            "pendingToken" to token
                        )
                    )
                } else {
                    viewModel.join(settlementId!!, token)   // 사용자가 명시적으로 동의한 순간에만 join
                }
            }
        }
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
                            findNavController().navigate(R.id.action_joinConfirm_to_roundPick)
                        }
                        is QrScanViewModel.JoinState.Error -> {
                            setLoading(false)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnJoin.isEnabled = !loading
        binding.btnCancel.isEnabled = !loading
    }

    private fun goHome() {
        findNavController().navigate(R.id.action_joinConfirm_to_home)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SETTLEMENT_ID = "settlementId"
        const val ARG_TOKEN = "token"
    }
}
