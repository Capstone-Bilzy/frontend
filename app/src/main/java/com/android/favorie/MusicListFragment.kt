package com.android.favorie

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.favorie.databinding.FragmentMusicListBinding
import com.android.favorie.network.RetrofitClient
import kotlinx.coroutines.launch

class MusicListFragment : Fragment(R.layout.fragment_music_list) {
    private var _binding: FragmentMusicListBinding? = null
    private val binding get() = _binding!!

    private var displayList: List<MusicItem> = emptyList()
    private var mainTab: String = "MUSIC"
    private var selectedCategory: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMusicListBinding.bind(view)

        mainTab = arguments?.getString("mainTab") ?: "MUSIC"
        selectedCategory = arguments?.getString("category") // 무드 태그 클릭 시 넘어옴

        binding.rvMusicList.apply {
            layoutManager = if (mainTab == "MUSIC") {
                LinearLayoutManager(context)
            } else {
                GridLayoutManager(context, 3)
            }
            // 빈 어댑터로 먼저 세팅 후 데이터 로드
            adapter = MusicListAdapter(emptyList()) { item -> navigateToDetail(item) }
        }

        loadItemsFromServer()
    }

    // 앱 내부 탭명 → API 카테고리명 변환
    private fun toApiCategory(mainTab: String) = when (mainTab) {
        "SPACE" -> "PLACE"
        "VIBE"  -> "MOOD"
        else    -> mainTab   // MUSIC, MOVIE, BOOK, FASHION은 동일
    }

    private fun loadItemsFromServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getItems(category = toApiCategory(mainTab))
                if (response.isSuccessful) {
                    val body = response.body()
                    val serverItems = body?.items?.map { item ->
                        MusicItem(
                            id         = item.id.toString(),
                            title      = item.title,
                            artist     = "",
                            imageUrl   = item.imageUrl ?: "",
                            userText   = item.userText,
                            category   = item.category,
                            mainTab    = mainTab,
                            globalTags = item.globalTags ?: emptyList(),
                            localTags  = item.localTags ?: emptyList()
                        )
                    } ?: emptyList()

                    // 소분류 필터: API 카테고리명 또는 태그명으로 필터링
                    displayList = if (selectedCategory != null) {
                        serverItems.filter { item ->
                            item.category == selectedCategory ||
                            item.globalTags.contains(selectedCategory) ||
                            item.localTags.contains(selectedCategory)
                        }
                    } else {
                        serverItems
                    }

                    binding.rvMusicList.adapter = MusicListAdapter(displayList) { item ->
                        navigateToDetail(item)
                    }
                } else {
                    Log.e("MusicListFragment", "아이템 조회 실패: ${response.code()}")
                    Toast.makeText(context, "목록을 불러오지 못했습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MusicListFragment", "아이템 조회 예외", e)
                Toast.makeText(context, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDetail(item: MusicItem) {
        val position = displayList.indexOf(item)
        (parentFragment as? OnMusicClickListener)?.openDetail(mainTab, selectedCategory, position)
    }


    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
