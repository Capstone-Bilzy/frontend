package com.android.bilzy.ui.room

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentCalculatingBinding

class CalculatingFragment : Fragment() {

    private var _binding: FragmentCalculatingBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler.postDelayed({
            if (_binding != null) {
                findNavController().navigate(R.id.action_calculating_to_settlementResult)
            }
        }, 2500L)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
