package com.android.favorie

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryGridFragment : Fragment(R.layout.fragment_music_list) {

    // 현재 프래그먼트가 어떤 카테고리인지 구분 (예: "mood", "movie", "book")
    private var currentCategory: String = "mood"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_music_list)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        // [DB 연동 포인트]
        // 1. items 테이블에서 category가 currentCategory인 것을 찾는다.
        // 2. 그 중 각 title(또는 local_tag)별로 가장 created_at이 최신인 것을 fetch한다.
        val dataList = fetchCategoryDataFromDB(currentCategory)

        recyclerView.adapter = CommonGridAdapter(dataList) { selectedItem ->
            // 클릭 시 해당 상세 리스트로 이동
            navigateToDetail(selectedItem)
        }
    }

    private fun fetchCategoryDataFromDB(category: String): List<CategoryItem> {
        // 임시 더미 데이터 (실제로는 ViewModel을 통해 DB 쿼리 결과 반환)
        return listOf(
            CategoryItem(1, "발견의 기쁨", "https://example.com/recent_music.jpg", "mood"),
            CategoryItem(2, "스릴러 장르", "https://example.com/recent_movie.jpg", "movie")
        )
    }

    private fun navigateToDetail(item: CategoryItem) {
        // 상세 화면 전환 로직
    }
}