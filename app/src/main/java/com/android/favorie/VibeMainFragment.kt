package com.android.favorie

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment

class VibeMainFragment : Fragment(R.layout.fragment_vibe_main),
    OnMusicClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvMusic = view.findViewById<TextView>(R.id.btn_by_vibe)
        val tvMood = view.findViewById<TextView>(R.id.btn_by_mood)
        val indicator = view.findViewById<View>(R.id.view_selected_bg)
        val layout = view.findViewById<ConstraintLayout>(R.id.cl_tab_controller)

        childFragmentManager.addOnBackStackChangedListener {
            layout.visibility = if (childFragmentManager.backStackEntryCount == 0) View.VISIBLE else View.GONE
        }

        // 초기 화면
        replaceChildFragment(MusicListFragment(), "VIBE")

        tvMusic.setOnClickListener {
            moveIndicator(layout, indicator, true)
            tvMusic.setTextColor(Color.BLACK)
            tvMood.setTextColor(Color.WHITE)
            replaceChildFragment(MusicListFragment(), "VIBE")
        }

        tvMood.setOnClickListener {
            moveIndicator(layout, indicator, false)
            tvMood.setTextColor(Color.BLACK)
            tvMusic.setTextColor(Color.WHITE)
            replaceChildFragment(MusicMoodFragment(), "VIBE")
        }
    }

    private fun moveIndicator(
        layout: ConstraintLayout,
        indicator: View,
        isLeft: Boolean
    ) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(layout)

        if (isLeft) {
            constraintSet.connect(
                indicator.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            constraintSet.connect(
                indicator.id,
                ConstraintSet.END,
                R.id.btn_by_mood,
                ConstraintSet.START
            )
        } else {
            constraintSet.connect(
                indicator.id,
                ConstraintSet.START,
                R.id.btn_by_vibe,
                ConstraintSet.END
            )
            constraintSet.connect(
                indicator.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
        }

        android.transition.TransitionManager.beginDelayedTransition(layout)
        constraintSet.applyTo(layout)
    }

    private fun replaceChildFragment(fragment: Fragment, mainTab: String) {
        fragment.arguments = Bundle().apply {
            putString("mainTab", mainTab)
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container, fragment)
            .commit()
    }

    // ⭐ 핵심: 여기서만 화면 전환
    override fun openDetail(mainTab: String, category: String?, position: Int) {
        val fragment = MusicDetailFragment().apply {
            arguments = Bundle().apply {
                putString("mainTab", mainTab)
                putString("category", category)
                putInt("startPosition", position)
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}