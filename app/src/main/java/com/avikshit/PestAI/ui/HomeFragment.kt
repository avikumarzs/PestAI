package com.avikshit.PestAI.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.avikshit.PestAI.R
import com.avikshit.PestAI.SprayEngine
import com.avikshit.PestAI.WeatherApi
import com.avikshit.PestAI.data.AppDatabase
import com.avikshit.PestAI.data.ScanRepository
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var scanRepository: ScanRepository
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    // IMPORTANT: Replace with your actual API key
    private val WEATHER_API_KEY = "7f0a84a0ced0506d40e5485da6acbf09"

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            // Permission has been granted.
            fetchLocationAndWeather()
        } else {
            // Permission was denied.
            val weatherWarning = view?.findViewById<TextView>(R.id.tvWeatherWarning)
            if (weatherWarning != null) {
                 if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // User has selected "Don't ask again". Guide them to settings.
                    weatherWarning.text = "Location permission is required. Please enable it in app settings."
                } else {
                    // User denied once.
                    weatherWarning.text = "Location permission is required for weather updates."
                }
                weatherWarning.isVisible = true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanRepository = ScanRepository(AppDatabase.getInstance(requireContext()).scanDao())

        view.findViewById<FloatingActionButton>(R.id.btnLaunchScanner).setOnClickListener {
            findNavController().navigate(R.id.scanFragment)
        }

        loadDashboard(view)
        checkLocationPermissions()
    }

    override fun onResume() {
        super.onResume()
        // ONLY update the UI dashboard here. Do NOT launch permission requests here!
        view?.let { loadDashboard(it) }
        
        // Silently check if they granted it in settings while the app was minimized
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdown()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            fetchLocationAndWeather()
        } else {
            // Request the permission
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun fetchLocationAndWeather() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If permissions are not granted, do not proceed.
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                checkSprayConditions(WEATHER_API_KEY, location.latitude, location.longitude)
            } else {
                val weatherWarning = view?.findViewById<TextView>(R.id.tvWeatherWarning)
                weatherWarning?.text = "Could not retrieve location. Please ensure location is enabled on your device."
                weatherWarning?.isVisible = true
            }
        }.addOnFailureListener {
            val weatherWarning = view?.findViewById<TextView>(R.id.tvWeatherWarning)
            weatherWarning?.text = "Failed to get location."
            weatherWarning?.isVisible = true
        }
    }

    private fun checkSprayConditions(apiKey: String, lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val weatherApi = WeatherApi.create()
                val response = withContext(Dispatchers.IO) {
                    weatherApi.getCurrentWeather(lat = lat, lon = lon, apiKey = apiKey)
                }

                val windSpeedKmh = response.wind.speed * 3.6
                val tempCelsius = response.main.temp
                val isRaining = response.weather.any { it.description.contains("rain", ignoreCase = true) }
                val decision = SprayEngine.evaluateConditions(windSpeedKmh, tempCelsius, isRaining)

                withContext(Dispatchers.Main) {
                    val weatherState = view?.findViewById<TextView>(R.id.tvWeatherState)
                    val weatherWarning = view?.findViewById<TextView>(R.id.tvWeatherWarning)
                    val temperatureView = view?.findViewById<TextView>(R.id.tvTemperature)
                    val windSpeedView = view?.findViewById<TextView>(R.id.tvWindSpeed)
                    val locationView = view?.findViewById<TextView>(R.id.tvLocation)
                    
                    if (weatherState == null || weatherWarning == null || temperatureView == null || windSpeedView == null || locationView == null) return@withContext
                    
                    weatherState.text = "Ready for Spray?"
                    weatherWarning.isVisible = true
                    weatherWarning.text = decision.message

                    temperatureView.text = String.format("%.1f°C", tempCelsius)
                    windSpeedView.text = String.format("%.1f km/h", windSpeedKmh)
                    locationView.text = response.name

                    when (decision.status) {
                        SprayEngine.Status.PERFECT -> {
                            weatherWarning.setBackgroundResource(R.color.kr_green_light)
                            weatherWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.kr_text_primary))
                        }
                        SprayEngine.Status.WARNING -> {
                            weatherWarning.setBackgroundResource(R.color.warning_yellow)
                            weatherWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        }
                        SprayEngine.Status.DO_NOT_SPRAY -> {
                            weatherWarning.setBackgroundResource(R.color.kr_red_critical)
                            weatherWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val weatherWarning = view?.findViewById<TextView>(R.id.tvWeatherWarning)
                    weatherWarning?.text = "Could not fetch weather data. Check connection or API key."
                    weatherWarning?.isVisible = true
                }
            }
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
