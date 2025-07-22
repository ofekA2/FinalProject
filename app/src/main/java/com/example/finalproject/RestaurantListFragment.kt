package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.databinding.FragmentRestaurantListBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
        db.collection("reviews").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener{ snapshot, e ->
            val list = snapshot?.documents?.map { doc ->
                doc.toObject(Review::class.java)!!.copy(id=doc.id)
            }?: emptyList()
            adapter.setData(list)
            }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}