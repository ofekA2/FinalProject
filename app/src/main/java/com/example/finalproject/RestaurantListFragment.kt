package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.databinding.FragmentRestaurantListBinding

class RestaurantListFragment: Fragment() {
    private var _binding: FragmentRestaurantListBinding? = null
    private val binding get() = _binding!!

    private val sampleData = listOf(
        "Taizu – Tel Aviv",
        "Machneyuda – Jerusalem",
        "Dolphin Yam – Nahariya"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SimpleStringAdapter(sampleData)
        binding.rvRestaurants.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRestaurants.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}