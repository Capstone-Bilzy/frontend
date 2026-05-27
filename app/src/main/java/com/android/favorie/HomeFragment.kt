package com.android.favorie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import com.android.favorie.databinding.FragmentHomeBinding
import com.android.favorie.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 카운트 0, 아이템 없는 상태로 먼저 렌더
        setupRecyclerView(0, 0, 0, 0, 0, 0, emptyMap())
        loadFromServer()
    }

    private fun loadFromServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 홈 카운트 조회
                val homeResp = RetrofitClient.api.getHome()
                if (!homeResp.isSuccessful) {
                    Log.e("HomeFragment", "홈 조회 실패: ${homeResp.code()} ${homeResp.errorBody()?.string()}")
                    if (homeResp.code() == 401) {
                        TokenManager.clear()
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "홈 로드 실패 (${homeResp.code()})", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val counts = homeResp.body()!!

                // 카테고리별 최근 아이템 병렬 조회 (최대 6개씩 미리보기용)
                val musicD   = async { fetchItems("MUSIC",   "MUSIC") }
                val movieD   = async { fetchItems("MOVIE",   "MOVIE") }
                val bookD    = async { fetchItems("BOOK",    "BOOK") }
                val spaceD   = async { fetchItems("PLACE",   "SPACE") }
                val fashionD = async { fetchItems("FASHION", "FASHION") }
                val vibeD    = async { fetchItems("MOOD",    "VIBE") }

                val itemsMap = mapOf(
                    "MUSIC"   to musicD.await(),
                    "MOVIE"   to movieD.await(),
                    "BOOK"    to bookD.await(),
                    "SPACE"   to spaceD.await(),
                    "FASHION" to fashionD.await(),
                    "VIBE"    to vibeD.await()
                )

                setupRecyclerView(
                    counts.music, counts.movie, counts.book,
                    counts.space, counts.fashion, counts.vibe,
                    itemsMap
                )
            } catch (e: Exception) {
                Log.e("HomeFragment", "홈 로드 예외", e)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 카테고리별 아이템 조회 후 MusicItem으로 변환
    private suspend fun fetchItems(apiCategory: String, mainTab: String): List<MusicItem> {
        return try {
            RetrofitClient.api.getItems(category = apiCategory, size = 6)
                .body()?.items?.map { item ->
                    MusicItem(
                        id       = item.id.toString(),
                        title    = item.title,
                        artist   = "",
                        imageUrl = item.imageUrl ?: "",
                        userText = item.userText,
                        category = item.category,
                        mainTab  = mainTab
                    )
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun setupRecyclerView(
        music: Int, movie: Int, book: Int,
        space: Int, fashion: Int, vibe: Int,
        itemsMap: Map<String, List<MusicItem>>
    ) {
        val cardItems = listOf(
            MainCard("MUSIC",   "#EBCB00", music,   R.drawable.ic_music,   R.drawable.ic_card_yellow, items = itemsMap["MUSIC"]   ?: emptyList()),
            MainCard("MOVIE",   "#FF4B4B", movie,   R.drawable.ic_movie,   R.drawable.ic_card_red,    items = itemsMap["MOVIE"]   ?: emptyList()),
            MainCard("BOOK",    "#00C853", book,    R.drawable.ic_book,    R.drawable.ic_card_green,  items = itemsMap["BOOK"]    ?: emptyList()),
            MainCard("SPACE",   "#FF8C00", space,   R.drawable.ic_space,   R.drawable.ic_card_orange, items = itemsMap["SPACE"]   ?: emptyList()),
            MainCard("FASHION", "#2196F3", fashion, R.drawable.ic_fashion, R.drawable.ic_card_blue,   items = itemsMap["FASHION"] ?: emptyList()),
            MainCard("VIBE",    "#9C27B0", vibe,    R.drawable.ic_vibe,    R.drawable.ic_card_purple, items = itemsMap["VIBE"]    ?: emptyList())
        )

        val mainCardAdapter = MainCardAdapter(
            items = cardItems,
            onCardClick = { position, isExpanding ->
                if (isExpanding) {
                    // 카드 확장 시 해당 카테고리를 FAB에 반영
                    (activity as? MainActivity)?.setExpandedCategory(cardItems[position].name)
                } else {
                    (activity as? MainActivity)?.setExpandedCategory(null)
                    navigateToCategory(cardItems[position].name)
                }
            },
            onItemClick = { item, position -> navigateToDetail(item, position) }
        )

        binding.rvMainCards.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mainCardAdapter
            clipToPadding = false
            clipChildren = false
        }
    }

    private fun navigateToDetail(item: MusicItem, position: Int) {
        val fragment = MusicDetailFragment().apply {
            arguments = android.os.Bundle().apply {
                putString("mainTab", item.mainTab)
                putString("category", null)
                putInt("startPosition", position)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToCategory(categoryName: String) {
        val targetFragment = when (categoryName) {
            "MUSIC"   -> MusicMainFragment()
            "MOVIE"   -> MovieMainFragment()
            "BOOK"    -> BookMainFragment()
            "SPACE"   -> SpaceMainFragment()
            "FASHION" -> FashionMainFragment()
            "VIBE"    -> VibeMainFragment()
            else      -> MusicMainFragment()
        }
        (activity as? MainActivity)?.navigateCategoryFragment(targetFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}