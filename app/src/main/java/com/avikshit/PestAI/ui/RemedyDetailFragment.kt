package com.avikshit.PestAI.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.avikshit.PestAI.R

class RemedyDetailFragment : Fragment(R.layout.fragment_remedy_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pestKey = arguments?.getString("pestKey") ?: ""

        val item = RemedyData.findByKey(pestKey)

        val tvPestName = view.findViewById<TextView>(R.id.tvPestName)
        val tvRemedyName = view.findViewById<TextView>(R.id.tvRemedyName)
        val tvEffectiveness = view.findViewById<TextView>(R.id.tvEffectiveness)
        val tvCost = view.findViewById<TextView>(R.id.tvCost)
        val tvIngredients = view.findViewById<TextView>(R.id.tvIngredients)
        val tvSteps = view.findViewById<TextView>(R.id.tvSteps)
        val tvGenericMessage = view.findViewById<TextView>(R.id.tvGenericMessage)

        if (item != null) {
            tvPestName.text = item.pestName
            tvRemedyName.text = item.remedyName
            tvEffectiveness.text = getString(R.string.remedy_effectiveness) + ": ${item.effectiveness}"
            tvCost.text = getString(R.string.remedy_cost) + ": ${item.cost}"
            tvIngredients.text = item.ingredients
            tvSteps.text = item.steps
            tvGenericMessage.isVisible = false
        } else {
            tvPestName.text = pestKey.replaceFirstChar { it.uppercase() }
            tvRemedyName.isVisible = false
            tvEffectiveness.isVisible = false
            tvCost.isVisible = false
            tvIngredients.isVisible = false
            tvSteps.isVisible = false
            tvGenericMessage.isVisible = true
            tvGenericMessage.text = "No specific remedy in database for this pest. Check general integrated pest management practices."
        }
    }
}
