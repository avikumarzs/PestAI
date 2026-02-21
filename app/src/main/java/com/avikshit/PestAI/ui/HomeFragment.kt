package com.avikshit.PestAI.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.avikshit.PestAI.R
import com.avikshit.PestAI.data.AppDatabase
import com.avikshit.PestAI.data.ScanRepository
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.util.concurrent.Executors

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var scanRepository: ScanRepository
    private val ioExecutor = Executors.newSingleThreadExecutor()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanRepository = ScanRepository(AppDatabase.getInstance(requireContext()).scanDao())

        val tvRecentScansSummary = view.findViewById<TextView>(R.id.tvRecentScansSummary)
        val fabStartScan = view.findViewById<ExtendedFloatingActionButton>(R.id.fabStartScan)

        fabStartScan.setOnClickListener {
            findNavController().navigate(R.id.scanFragment)
        }

        loadSummary(tvRecentScansSummary)
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.tvRecentScansSummary)?.let { loadSummary(it) }
    }

    private fun loadSummary(summaryTextView: TextView) {
        ioExecutor.execute {
            val scanCount = scanRepository.getScanCount()
            activity?.runOnUiThread {
                summaryTextView.text = getString(R.string.recent_scans_count, scanCount)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdown()
    }
}
