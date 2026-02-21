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
import com.avikshit.PestAI.data.ScanEntity
import com.avikshit.PestAI.data.ScanRepository
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.util.concurrent.Executors

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var scanRepository: ScanRepository
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val historyAdapter = HistoryAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanRepository = ScanRepository(AppDatabase.getInstance(requireContext()).scanDao())

        view.findViewById<ExtendedFloatingActionButton>(R.id.fabStartScan).setOnClickListener {
            findNavController().navigate(R.id.scanFragment)
        }

        val rvRecentScans = view.findViewById<RecyclerView>(R.id.rvRecentScans)
        rvRecentScans.layoutManager = LinearLayoutManager(requireContext())
        rvRecentScans.adapter = historyAdapter

        loadDashboard(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadDashboard(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdown()
    }

    private fun loadDashboard(rootView: View) {
        val tvMetricCritical = rootView.findViewById<TextView>(R.id.tvMetricCritical)
        val tvMetricWarning = rootView.findViewById<TextView>(R.id.tvMetricWarning)
        val tvMetricTotal = rootView.findViewById<TextView>(R.id.tvMetricTotal)
        val rvRecentScans = rootView.findViewById<RecyclerView>(R.id.rvRecentScans)
        val tvEmptyState = rootView.findViewById<TextView>(R.id.tvEmptyState)

        ioExecutor.execute {
            val scans = scanRepository.getAllScans()
            val criticalCount = scans.count { isCriticalPest(it.pestName) }
            val warningCount = scans.size - criticalCount

            activity?.runOnUiThread {
                tvMetricCritical.text = criticalCount.toString()
                tvMetricWarning.text = warningCount.toString()
                tvMetricTotal.text = scans.size.toString()
                historyAdapter.submitList(scans)
                val isEmpty = scans.isEmpty()
                tvEmptyState.isVisible = isEmpty
                rvRecentScans.isVisible = !isEmpty
            }
        }
    }

    private fun isCriticalPest(pestName: String): Boolean {
        val lower = pestName.lowercase()
        return lower.contains("armyworm") || lower.contains("stem borer") || lower.contains("borer")
    }
}
