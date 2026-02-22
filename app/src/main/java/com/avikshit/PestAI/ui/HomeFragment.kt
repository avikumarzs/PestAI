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
import androidx.preference.PreferenceManager
import com.avikshit.PestAI.R
import com.avikshit.PestAI.SprayEngine
import com.avikshit.PestAI.WeatherApi
import com.avikshit.PestAI.data.AppDatabase
import com.avikshit.PestAI.data.ScanRepository
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import android.widget.ProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var scanRepository: ScanRepository
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    // IMPORTANT: Replace with your actual API key
    private val WEATHER_API_KEY = "672e0ac8f2a6506c03115e76033b8dd1"

    // Local Arduino/Flask sensor server (cleartext; ensure phone and laptop on same network)
    private val SENSOR_BASE_URL = "http://172.24.239.67:5000"

    private var sensorPollingJob: Job? = null
    private val gson = Gson()

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

        view.findViewById<View>(R.id.btnWeatherRetry).setOnClickListener {
            setWeatherLoading(true)
            fetchLocationAndWeather()
        }

        loadDashboard(view)
        checkLocationPermissions()
        startSensorPolling()
    }

    override fun onDestroyView() {
        sensorPollingJob?.cancel()
        sensorPollingJob = null
        super.onDestroyView()
    }

    private fun startSensorPolling() {
        sensorPollingJob?.cancel()
        sensorPollingJob = lifecycleScope.launch {
            while (true) {
                fetchSensorData()
                delay(2000L)
            }
        }
    }

    private suspend fun fetchSensorData() {
        val result = withContext(Dispatchers.IO) {
            try {
                val url = URL("$SENSOR_BASE_URL/data")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    return@withContext null
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                gson.fromJson(text, SensorData::class.java)
            } catch (_: Exception) {
                null
            }
        }
        withContext(Dispatchers.Main) {
            val tvTemp = view?.findViewById<TextView>(R.id.tvTemperature)
            val tvHumidity = view?.findViewById<TextView>(R.id.tvHumidity)
            val tvLdr = view?.findViewById<TextView>(R.id.tvLdr)
            val tvDistance = view?.findViewById<TextView>(R.id.tvDistance)
            val tvDisconnected = view?.findViewById<TextView>(R.id.tvSensorsDisconnected)
            if (result != null) {
                tvTemp?.text = formatSensorTemp(result.temperature)
                tvHumidity?.text = formatSensorHumidity(result.humidity)
                tvLdr?.text = result.ldr ?: "--"
                tvDistance?.text = formatSensorDistance(result.distance)
                tvDisconnected?.isVisible = false
            } else {
                tvTemp?.text = "--°C"
                tvHumidity?.text = "--%"
                tvLdr?.text = "--"
                tvDistance?.text = "--"
                tvDisconnected?.isVisible = true
            }
        }
    }

    private fun formatSensorTemp(s: String?): String {
        if (s.isNullOrBlank()) return "--°C"
        return try { "${s.trim()}°C" } catch (_: Exception) { "--°C" }
    }

    private fun formatSensorHumidity(s: String?): String {
        if (s.isNullOrBlank()) return "--%"
        return try { "${s.trim()}%" } catch (_: Exception) { "--%" }
    }

    private fun formatSensorDistance(s: String?): String {
        if (s.isNullOrBlank()) return "--"
        return try {
            val d = s.trim().toDoubleOrNull()
            if (d != null) "%.2f".format(d) else "--"
        } catch (_: Exception) { "--" }
    }

    override fun onResume() {
        super.onResume()
        // ONLY update the UI dashboard here. Do NOT launch permission requests here!
        view?.let { loadDashboard(it) }
        updateGreeting()
        
        // Silently check if they granted it in settings while the app was minimized
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdown()
    }

    private fun updateGreeting() {
        val greetingView = view?.findViewById<TextView>(R.id.tvGreeting)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val farmerName = sharedPreferences.getString("farmer_name", "")
        if (!farmerName.isNullOrEmpty()) {
            greetingView?.text = "Hi, $farmerName"
        } else {
            greetingView?.text = getString(R.string.home_greeting)
        }
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

    private fun setWeatherLoading(loading: Boolean) {
        val weatherState = view?.findViewById<TextView>(R.id.tvWeatherState)
        val locationView = view?.findViewById<TextView>(R.id.tvLocation)
        val weatherWarning = view?.findViewById<TextView>(R.id.tvWeatherWarning)
        val btnRetry = view?.findViewById<View>(R.id.btnWeatherRetry)
        val progress = view?.findViewById<ProgressBar>(R.id.weatherProgress)
        if (loading) {
            weatherState?.text = getString(R.string.weather_fetching)
            locationView?.text = getString(R.string.weather_fetching)
            weatherWarning?.isVisible = false
            btnRetry?.isEnabled = false
            progress?.isVisible = true
        } else {
            btnRetry?.isEnabled = true
            progress?.isVisible = false
        }
    }

    private fun fetchLocationAndWeather() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            setWeatherLoading(false)
            updateWeatherUIError("Location permission required", "Enable location in app settings.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    checkSprayConditions(WEATHER_API_KEY, location.latitude, location.longitude, null)
                } else {
                    val fallbackLat = 28.6139
                    val fallbackLon = 77.2090
                    checkSprayConditions(WEATHER_API_KEY, fallbackLat, fallbackLon, "Default location (India)")
                }
            }
            .addOnFailureListener {
                setWeatherLoading(false)
                updateWeatherUIError("Location unavailable", "Could not get GPS. Using default location.")
                val fallbackLat = 28.6139
                val fallbackLon = 77.2090
                checkSprayConditions(WEATHER_API_KEY, fallbackLat, fallbackLon, "Default location (India)")
            }
    }

    private fun checkSprayConditions(apiKey: String, lat: Double, lon: Double, fallbackLocationName: String?) {
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
                    val locationView = view?.findViewById<TextView>(R.id.tvLocation)

                    if (weatherState == null || weatherWarning == null || locationView == null) return@withContext

                    setWeatherLoading(false)
                    weatherState.text = getString(R.string.weather_card_title)
                    weatherWarning.isVisible = true
                    weatherWarning.text = decision.message
                    locationView.text = fallbackLocationName ?: response.name
                    // Temperature and humidity in this card are from local sensor (polled separately)

                    when (decision.status) {
                        SprayEngine.Status.PERFECT -> {
                            weatherWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kr_green_light))
                            weatherWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                        }
                        SprayEngine.Status.WARNING -> {
                            weatherWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow))
                            weatherWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        }
                        SprayEngine.Status.DO_NOT_SPRAY -> {
                            weatherWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.critical_soft))
                            weatherWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setWeatherLoading(false)
                    updateWeatherUIError("Weather unavailable", "Could not fetch weather data. Check connection or API key.")
                }
            }
        }
    }

    private fun updateWeatherUIError(stateText: String, warningText: String) {
        val weatherState = view?.findViewById<TextView>(R.id.tvWeatherState)
        val weatherWarning = view?.findViewById<TextView>(R.id.tvWeatherWarning)
        val temperatureView = view?.findViewById<TextView>(R.id.tvTemperature)
        val humidityView = view?.findViewById<TextView>(R.id.tvHumidity)
        val locationView = view?.findViewById<TextView>(R.id.tvLocation)

        weatherState?.text = stateText
        weatherWarning?.text = warningText
        weatherWarning?.isVisible = true
        temperatureView?.text = "--°C"
        humidityView?.text = "--%"
        locationView?.text = "Location unavailable"
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
        val key = pestName.replace(" ", "_").lowercase()
        return key == "army_fallworm" || key == "grain_weevil"
    }
}

/**
 * JSON from local Flask server: {"temperature": "29.00", "humidity": "40.00", "ldr": "29", "distance": "0.00"}
 */
private data class SensorData(
    val temperature: String? = null,
    val humidity: String? = null,
    val ldr: String? = null,
    val distance: String? = null
)
