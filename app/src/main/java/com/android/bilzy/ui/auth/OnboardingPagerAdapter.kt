package com.android.bilzy.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.R

data class OnboardingPage(val title: String, val subtitle: String)

class OnboardingPagerAdapter : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    private val pages = listOf(
        OnboardingPage(
            "영수증 스캔부터 맞춤 정산까지,\nBilzy 하나면 충분해요!",
            "복잡한 계산 없이 정산을 간편하게 끝내보세요"
        ),
        OnboardingPage(
            "AI가 분석하는\n우리만의 스마트한 정산 리포트",
            "술 안 마신 친구, 늦게 온 친구까지 섬세하게 반영해요"
        ),
        OnboardingPage(
            "모임이 끝났을 때,\n정산도 끝!",
            "모임 후 번거로운 정산 스트레스를 줄여보세요"
        )
    )

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.tvTitle.text = page.title
        holder.tvSubtitle.text = page.subtitle
    }

    override fun getItemCount() = pages.size
}
