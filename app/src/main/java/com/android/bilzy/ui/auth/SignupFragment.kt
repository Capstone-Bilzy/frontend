package com.android.bilzy.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnKakao.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_signupKakaoTerms)
        }

        binding.btnNaver.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_signupNaverTerms)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
