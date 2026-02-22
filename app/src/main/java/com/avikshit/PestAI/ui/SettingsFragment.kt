package com.avikshit.PestAI.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.avikshit.PestAI.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}