package com.avikshit.PestAI.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.avikshit.PestAI.R

/**
 * Organic Remedies screen with 4 hardcoded pest cards:
 * Fall Armyworm (Critical), Aphids (Warning), Leafhoppers (Warning), Stem Borer (Critical).
 * Content is fully defined in fragment_remedies.xml.
 */
class RemediesFragment : Fragment(R.layout.fragment_remedies) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // All content is static in XML; no runtime logic required.
    }
}
