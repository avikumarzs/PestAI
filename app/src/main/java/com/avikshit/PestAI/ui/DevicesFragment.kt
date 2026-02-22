package com.avikshit.PestAI.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.avikshit.PestAI.R

/**
 * Rover telemetry dashboard: DHT11 (temperature, humidity) and Light sensor.
 * Values are mock; can be replaced with real BLE/HTTP data later.
 */
class DevicesFragment : Fragment(R.layout.fragment_devices) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // UI shows mock 32°C, 65%, 40% Optimal; no runtime logic required.
    }
}
