package com.android.bilzy.ui.mypage

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.databinding.FragmentMyPageAccountBinding
import com.android.bilzy.domain.model.BankAccount
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyPageAccountFragment : Fragment() {

    private var _binding: FragmentMyPageAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyPageAccountViewModel by viewModels()

    /** 로딩된 계좌를 한 번만 입력칸에 채우기 위한 플래그(사용자가 편집 중일 때 덮어쓰지 않도록). */
    private var prefilled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBankSpinner()
        observeAccount()
        observeSaveState()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            viewModel.save(
                bankName = binding.spinnerBank.selectedItem?.toString().orEmpty(),
                accountNumber = binding.etAccountNumber.text?.toString().orEmpty(),
                accountHolder = binding.etAccountName.text?.toString().orEmpty()
            )
        }
    }

    private fun setupBankSpinner() {
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            MyPageAccountViewModel.BANKS
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? TextView)?.setTextColor(Color.WHITE)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as? TextView)?.setTextColor(Color.WHITE)
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBank.adapter = adapter
        binding.spinnerBank.setSelection(0)
    }

    private fun observeAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.account.collect { account ->
                    account ?: return@collect
                    renderSummary(account)
                    if (!prefilled) {
                        prefill(account)
                        prefilled = true
                    }
                }
            }
        }
    }

    private fun observeSaveState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    when (state) {
                        is MyPageAccountViewModel.SaveState.Success -> {
                            toast("계좌 정보를 저장했어요")
                            viewModel.consumeSaveState()
                            findNavController().navigateUp()
                        }
                        is MyPageAccountViewModel.SaveState.Error -> {
                            toast(state.message)
                            viewModel.consumeSaveState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    /** 상단 '대표 계좌' 요약 카드를 실데이터로. 미설정이면 안내 문구. */
    private fun renderSummary(account: BankAccount) {
        if (account.isEmpty) {
            binding.tvCurrentBank.text = "등록된 계좌 없음"
            binding.tvCurrentNumber.text = "-"
            binding.tvCurrentName.text = "-"
        } else {
            binding.tvCurrentBank.text = account.bankName.ifBlank { "-" }
            binding.tvCurrentNumber.text = account.accountNumber.ifBlank { "-" }
            binding.tvCurrentName.text = account.accountHolder.ifBlank { "-" }
        }
    }

    /** 저장된 계좌를 입력 폼에 미리 채움(편집 출발점). */
    private fun prefill(account: BankAccount) {
        val bankIndex = MyPageAccountViewModel.BANKS.indexOf(account.bankName)
        if (bankIndex >= 0) binding.spinnerBank.setSelection(bankIndex)
        binding.etAccountNumber.setText(account.accountNumber)
        binding.etAccountName.setText(account.accountHolder)
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
