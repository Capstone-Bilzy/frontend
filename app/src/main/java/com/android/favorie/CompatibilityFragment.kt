package com.android.favorie

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.databinding.FragmentCompatibilityBinding
import com.android.favorie.network.RetrofitClient
import kotlinx.coroutines.launch

class CompatibilityFragment : Fragment() {

    private var _binding: FragmentCompatibilityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompatibilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadMyCode()

        binding.btnCopyCode.setOnClickListener { copyCode() }

        binding.btnCompare.setOnClickListener {
            val code = binding.etOpponentCode.text.toString().trim().uppercase()
            if (code.length != 8) {
                showError("8자리 코드를 입력해주세요.")
                return@setOnClickListener
            }
            runCompare(code)
        }
    }

    private fun loadMyCode() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getMyCompareCode()
                if (resp.isSuccessful) {
                    binding.tvMyCode.text = resp.body()?.code ?: "-"
                } else {
                    binding.tvMyCode.text = "오류"
                    Log.e("CompatibilityFragment", "코드 조회 실패: ${resp.code()}")
                }
            } catch (e: Exception) {
                binding.tvMyCode.text = "오류"
                Log.e("CompatibilityFragment", "코드 조회 예외", e)
            }
        }
    }

    private fun copyCode() {
        val code = binding.tvMyCode.text.toString()
        if (code.isBlank() || code == "불러오는 중…" || code == "오류") return
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("compare_code", code))
        Toast.makeText(requireContext(), "코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun runCompare(code: String) {
        showError("")
        binding.btnCompare.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.compare(code)
                when {
                    resp.isSuccessful -> {
                        val data = resp.body()!!
                        val resultFragment = CompatibilityResultFragment.newInstance(data)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, resultFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    resp.code() == 400 -> showError("자기 자신과는 비교할 수 없습니다.")
                    resp.code() == 404 -> showError("유효하지 않은 취향 궁합 코드입니다.")
                    resp.code() == 502 -> showError("취향 궁합 비교에 실패했습니다. 잠시 후 다시 시도해주세요.")
                    else               -> showError("오류가 발생했습니다. (${resp.code()})")
                }
            } catch (e: Exception) {
                showError("네트워크 오류가 발생했습니다.")
                Log.e("CompatibilityFragment", "비교 예외", e)
            } finally {
                binding.btnCompare.isEnabled = true
            }
        }
    }

    private fun showError(msg: String) {
        if (msg.isBlank()) {
            binding.tvError.visibility = View.GONE
        } else {
            binding.tvError.text = msg
            binding.tvError.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}