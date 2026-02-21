package com.avikshit.PestAI.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.avikshit.PestAI.R

class RemediesAdapter(
    private val onItemClick: (RemedyItem) -> Unit
) : RecyclerView.Adapter<RemediesAdapter.ViewHolder>() {

    private var items = listOf<RemedyItem>()

    fun submitList(newItems: List<RemedyItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_remedy_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val leftEdgeBar: View = itemView.findViewById(R.id.leftEdgeBar)
        private val tvPestName: TextView = itemView.findViewById(R.id.tvPestName)
        private val tvRemedyName: TextView = itemView.findViewById(R.id.tvRemedyName)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvBadge)

        fun bind(item: RemedyItem, onItemClick: (RemedyItem) -> Unit) {
            val ctx = itemView.context
            leftEdgeBar.setBackgroundColor(
                ContextCompat.getColor(ctx, if (item.badgeCritical) R.color.critical_red else R.color.warning_yellow)
            )
            tvPestName.text = item.pestName
            tvRemedyName.text = item.remedyName
            tvBadge.text = if (item.badgeCritical) "Critical" else "Warning"
            tvBadge.setBackgroundResource(
                if (item.badgeCritical) R.drawable.bg_badge_critical else R.drawable.bg_badge_warning
            )
            tvBadge.setTextColor(ContextCompat.getColor(ctx, if (item.badgeCritical) android.R.color.white else R.color.text_primary))
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
