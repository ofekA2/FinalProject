package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.databinding.FragmentRestaurantListBinding
import com.google.firebase.firestore.FirebaseFirestore

class RestaurantListFragment: Fragment() {
    private var _binding: FragmentRestaurantListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ReviewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRestaurantListBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        adapter = ReviewAdapter(emptyList())
        binding.rvRestaurants.layoutManager = LinearLayoutManager(context)
        binding.rvRestaurants.adapter = adapter
        db.collection("reviews").get().addOnSuccessListener {
            snapshot ->
            val reviews = snapshot.mapNotNull { doc -> doc.toObject(Review:: class.java) }
            adapter.setData(reviews)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load restaurants", Toast.LENGTH_SHORT).show()
            }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}