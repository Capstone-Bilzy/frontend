package com.android.bilzy.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val dots by lazy {
        listOf(binding.dot0, binding.dot1, binding.dot2)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = OnboardingPagerAdapter()
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }
        })

        updateDots(0)

        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_onboarding_to_login)
        }

        binding.btnSignup.setOnClickListener {
            findNavController().navigate(R.id.action_onboarding_to_signup)
        }
    }

    private fun updateDots(selectedIndex: Int) {
        dots.forEachIndexed { index, dot ->
            if (index == selectedIndex) {
                dot.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dot_active_width)
                dot.setBackgroundResource(R.drawable.bg_dot)
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#7C7AED")
                )
            } else {
                dot.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dot_inactive_width)
                dot.setBackgroundResource(R.drawable.bg_dot)
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#4DFFFFFF")
                )
            }
            dot.requestLayout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
