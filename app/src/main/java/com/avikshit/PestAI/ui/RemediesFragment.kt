package com.avikshit.PestAI.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avikshit.PestAI.R

class RemediesFragment : Fragment(R.layout.fragment_remedies) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvRemedies)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = RemediesAdapter { item ->
            findNavController().navigate(
                R.id.remedyDetailFragment,
                android.os.Bundle().apply { putString("pestKey", item.pestKey) }
            )
        }
        rv.adapter = adapter
        adapter.submitList(RemedyData.all())
    }
}
