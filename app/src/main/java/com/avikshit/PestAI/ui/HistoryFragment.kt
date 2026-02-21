package com.avikshit.PestAI.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avikshit.PestAI.R
import com.avikshit.PestAI.data.AppDatabase
import com.avikshit.PestAI.data.ScanRepository
import java.util.concurrent.Executors

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var scanRepository: ScanRepository
    private val historyAdapter = HistoryAdapter { scan ->
        val pestKey = RemedyData.normalizePestKey(scan.pestName)
        findNavController().navigate(
            R.id.remedyDetailFragment,
            android.os.Bundle().apply { putString("pestKey", pestKey) }
        )
    }
    private val ioExecutor = Executors.newSingleThreadExecutor()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scanRepository = ScanRepository(AppDatabase.getInstance(requireContext()).scanDao())

        val historyRecyclerView = view.findViewById<RecyclerView>(R.id.rvHistory)
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyRecyclerView.adapter = historyAdapter

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val rootView = view ?: return
        val emptyStateText = rootView.findViewById<TextView>(R.id.tvEmptyState)
        val historyRecyclerView = rootView.findViewById<RecyclerView>(R.id.rvHistory)

        ioExecutor.execute {
            val scans = scanRepository.getAllScans()
            activity?.runOnUiThread {
                historyAdapter.submitList(scans)
                val isEmpty = scans.isEmpty()
                emptyStateText.isVisible = isEmpty
                historyRecyclerView.isVisible = !isEmpty
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdown()
    }
}
