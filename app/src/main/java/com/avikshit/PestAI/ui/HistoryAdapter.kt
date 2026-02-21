package com.avikshit.PestAI.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avikshit.PestAI.R
import com.avikshit.PestAI.data.ScanEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    private val items = mutableListOf<ScanEntity>()
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

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
        holder.bind(items[position], dateFormatter)
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pestNameTextView: TextView = itemView.findViewById(R.id.tvPestName)
        private val timestampTextView: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(scanEntity: ScanEntity, formatter: SimpleDateFormat) {
            pestNameTextView.text = "${scanEntity.pestName} (${(scanEntity.confidence * 100).toInt()}%)"
            val timestampText = formatter.format(Date(scanEntity.timestamp))
            timestampTextView.text =
                itemView.context.getString(R.string.history_item_time, timestampText)
        }
    }
}
