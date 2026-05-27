package com.android.favorie

import android.graphics.Color
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.res.ColorStateList

class MainCardAdapter(
    private val items: List<MainCard>,
    private val onCardClick: (Int, Boolean) -> Unit,
    private val onItemClick: (MusicItem, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<MainCardAdapter.ViewHolder>() {

    private var expandedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutCard: ConstraintLayout = view.findViewById(R.id.layout_card_bg)
        val ivCardBgImage: ImageView = view.findViewById(R.id.iv_card_bg_image)
        val ivIcon: ImageView = view.findViewById(R.id.iv_card_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_card_title)
        val tvScore: TextView = view.findViewById(R.id.tv_card_score)
        val tvAddHint: TextView = view.findViewById(R.id.tv_add_favorite_hint)
        val rvCardItems: RecyclerView = view.findViewById(R.id.rv_card_items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val density = holder.itemView.resources.displayMetrics.density
        val cardColor = Color.parseColor(item.color)
        val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams

        if (expandedPosition == -1) {
            // 기본 스택 상태
            params.height = (220 * density).toInt()
            params.topMargin = if (position == 0) (20 * density).toInt() else (-150 * density).toInt()
            holder.itemView.visibility = View.VISIBLE
            holder.tvAddHint.visibility = View.VISIBLE
            holder.rvCardItems.visibility = View.GONE
        } else {
            if (expandedPosition == position) {
                // 선택된 카드: 전체 화면으로 확장
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                params.topMargin = (20 * density).toInt()
                holder.itemView.visibility = View.VISIBLE
                holder.tvAddHint.visibility = View.VISIBLE
                if (item.items.isEmpty()) {
                    holder.rvCardItems.visibility = View.GONE
                } else {
                    holder.rvCardItems.visibility = View.VISIBLE
                    holder.rvCardItems.layoutManager = if (item.name == "MUSIC") {
                        LinearLayoutManager(holder.itemView.context)
                    } else {
                        GridLayoutManager(holder.itemView.context, 3)
                    }
                    holder.rvCardItems.adapter = MusicListAdapter(item.items) { clickedItem ->
                        onItemClick(clickedItem, item.items.indexOf(clickedItem))
                    }
                }
            } else {
                params.height = 0
                params.topMargin = 0
                holder.itemView.visibility = View.GONE
                holder.rvCardItems.visibility = View.GONE
            }
        }
        holder.itemView.layoutParams = params

        holder.ivCardBgImage.setImageResource(item.cardImageRes)
        holder.tvTitle.text = item.name
        holder.tvScore.text = item.count.toString()

        if (item.iconRes != 0) {
            holder.ivIcon.setImageResource(item.iconRes)
            holder.ivIcon.imageTintList = ColorStateList.valueOf(cardColor)
            holder.ivIcon.visibility = View.VISIBLE
        }

        // "Add your favorie" 텍스트: 항상 카테고리 페이지로 이동
        holder.tvAddHint.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener
            onCardClick(currentPos, false)
        }

        // 카드 배경 영역 클릭
        holder.layoutCard.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            if (expandedPosition == currentPos) {
                // 이미 확장된 카드 클릭 → 카테고리 페이지로 이동
                onCardClick(currentPos, false)
            } else {
                // 접힌 카드 클릭 → 확장 애니메이션
                val recyclerView = holder.itemView.parent as ViewGroup
                TransitionManager.beginDelayedTransition(recyclerView, AutoTransition().setDuration(250))
                expandedPosition = currentPos
                onCardClick(currentPos, true)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = items.size
}