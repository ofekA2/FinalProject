package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.databinding.FragmentRestaurantListBinding
import com.google.firebase.Timestamp

class RestaurantListFragment: Fragment() {

    private var _binding: FragmentRestaurantListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val adapter = ReviewAdapter(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RestaurantListFragment.adapter
        }

        viewModel.reviews.observe(viewLifecycleOwner) { entities ->
            val uiReviews = entities.map { ent ->
                Review(
                    id = ent.id,
                    authorId = ent.authorId,
                    authorName = ent.authorName,
                    authorPhoto = ent.authorPhoto,
                    restaurant = ent.restaurant,
                    city = ent.city,
                    cuisine = ent.cuisine,
                    rating = ent.rating,
                    priceTier = ent.priceTier,
                    reviewText = ent.reviewText,
                    imageUrl = ent.imageUrl,
                    timestamp = Timestamp(
                        ent.timestampMs / 1_000,
                        ((ent.timestampMs % 1_000) * 1_000_000).toInt()
                    )
                )
            }
            adapter.setData(uiReviews)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}