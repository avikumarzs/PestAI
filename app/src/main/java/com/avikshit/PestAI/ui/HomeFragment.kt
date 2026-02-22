package com.avikshit.PestAI.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.avikshit.PestAI.R
import com.avikshit.PestAI.data.AppDatabase
import com.avikshit.PestAI.data.ScanRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executors

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var scanRepository: ScanRepository
    private val ioExecutor = Executors.newSingleThreadExecutor()

    /** Mock: true = show "Rain Tomorrow" and spray delay warning. */
    private val mockRainTomorrow = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanRepository = ScanRepository(AppDatabase.getInstance(requireContext()).scanDao())

        view.findViewById<FloatingActionButton>(R.id.btnLaunchScanner).setOnClickListener {
            findNavController().navigate(R.id.scanFragment)
        }

        setupWeatherCard(view)
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

    private fun setupWeatherCard(rootView: View) {
        val tvWeatherState = rootView.findViewById<TextView>(R.id.tvWeatherState)
        val tvWeatherWarning = rootView.findViewById<TextView>(R.id.tvWeatherWarning)
        if (mockRainTomorrow) {
            tvWeatherState.text = getString(R.string.weather_rain_tomorrow)
            tvWeatherWarning.isVisible = true
            tvWeatherWarning.text = "\uD83C\uDF27 " + getString(R.string.weather_rain_warning)
        } else {
            tvWeatherState.text = getString(R.string.weather_clear)
            tvWeatherWarning.isVisible = false
        }
    }

    private fun loadDashboard(rootView: View) {
        val tvMetricCritical = rootView.findViewById<TextView>(R.id.tvMetricCritical)
        val tvMetricTotal = rootView.findViewById<TextView>(R.id.tvMetricTotal)

        ioExecutor.execute {
            val scans = scanRepository.getAllScans()
            val criticalCount = scans.count { isCriticalPest(it.pestName) }

            activity?.runOnUiThread {
                tvMetricCritical.text = criticalCount.toString()
                tvMetricTotal.text = scans.size.toString()
            }
        }
    }

    private fun isCriticalPest(pestName: String): Boolean {
        val lower = pestName.lowercase()
        return lower.contains("armyworm") || lower.contains("stem borer") || lower.contains("borer")
    }
}
