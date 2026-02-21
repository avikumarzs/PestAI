package com.avikshit.PestAI.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.avikshit.PestAI.R
import com.avikshit.PestAI.data.ScanEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: ((ScanEntity) -> Unit)? = null
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    private val items = mutableListOf<ScanEntity>()
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun submitList(newItems: List<ScanEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position], dateFormatter, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val leftEdgeBar: View = itemView.findViewById(R.id.leftEdgeBar)
        private val pestNameTextView: TextView = itemView.findViewById(R.id.tvPestName)
        private val timestampTextView: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val confidencePill: TextView = itemView.findViewById(R.id.tvConfidencePill)

        fun bind(scanEntity: ScanEntity, formatter: SimpleDateFormat, onItemClick: ((ScanEntity) -> Unit)?) {
            val ctx = itemView.context
            val isCritical = isCriticalPest(scanEntity.pestName)
            leftEdgeBar.setBackgroundColor(
                ContextCompat.getColor(ctx, if (isCritical) R.color.critical_red else R.color.warning_yellow)
            )
            pestNameTextView.text = scanEntity.pestName
            timestampTextView.text = ctx.getString(R.string.history_item_time, formatter.format(Date(scanEntity.timestamp)))
            confidencePill.text = "${(scanEntity.confidence * 100).toInt()}%"
            itemView.setOnClickListener { onItemClick?.invoke(scanEntity) }
        }

        private fun isCriticalPest(pestName: String): Boolean {
            val lower = pestName.lowercase()
            return lower.contains("armyworm") || lower.contains("stem borer") || lower.contains("borer")
        }
    }
}
