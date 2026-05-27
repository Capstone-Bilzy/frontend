package com.android.favorie

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.favorie.network.RetrofitClient
import kotlinx.coroutines.launch

class MusicMoodFragment : Fragment(R.layout.fragment_music_mood) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvMood = view.findViewById<RecyclerView>(R.id.rv_mood_grid)
        val mainTab = arguments?.getString("mainTab") ?: "MUSIC"

        rvMood.layoutManager = GridLayoutManager(context, 2)
        rvMood.adapter = MoodGridAdapter(emptyList()) {}

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiCategory = when (mainTab) {
                    "SPACE" -> "PLACE"
                    "VIBE"  -> "MOOD"
                    else    -> mainTab
                }
                val response = RetrofitClient.api.getItems(category = apiCategory)
                if (response.isSuccessful) {
                    val items = response.body()?.items ?: emptyList()

                    // localTags(없으면 globalTags)를 기준으로 그룹핑, 대표 이미지는 첫 번째 아이템
                    val tagItems = items
                        .flatMap { item ->
                            val tags = item.localTags?.ifEmpty { item.globalTags }
                                ?: item.globalTags
                                ?: emptyList()
                            tags.map { tag -> tag to item }
                        }
                        .groupBy { (tag, _) -> tag }
                        .map { (tag, pairs) ->
                            val group = pairs.map { it.second }
                            MusicItem(
                                id       = tag,
                                title    = tag,
                                artist   = "",
                                imageUrl = group.firstOrNull { !it.imageUrl.isNullOrEmpty() }?.imageUrl ?: "",
                                userText = null,
                                category = tag,
                                mainTab  = mainTab
                            )
                        }

                    rvMood.adapter = MoodGridAdapter(tagItems) { clickedItem ->
                        openCategory(clickedItem.category)
                    }
                } else {
                    Log.e("MusicMoodFragment", "태그 조회 실패: ${response.code()}")
                    Toast.makeText(context, "태그를 불러오지 못했습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MusicMoodFragment", "태그 조회 예외", e)
                Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCategory(category: String?) {
        val mainTab = arguments?.getString("mainTab") ?: "MUSIC"

        val fragment = MusicListFragment().apply {
            arguments = Bundle().apply {
                putString("category", category)
                putString("mainTab", mainTab)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}