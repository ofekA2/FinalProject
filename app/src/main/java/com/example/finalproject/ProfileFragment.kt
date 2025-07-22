package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.finalproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReviewAdapter
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser ?: return

        binding.tvProfileName.text = user.displayName ?: "Anonymous"
        Glide.with(this)
            .load(user.photoUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .into(binding.ivProfilePic)

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(
                R.id.loginFragment, null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                }
            )
        }

        adapter = ReviewAdapter(emptyList())
        binding.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPosts.adapter = adapter

        adapter.onLongClick = { review ->
            showReviewMenu(review)
        }

        loadMyPosts(user.uid)
    }

    private fun loadMyPosts(uid: String) {
        db.collection("reviews")
            .whereEqualTo("authorId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { doc ->
                    doc.toObject(Review::class.java)!!.copy(id = doc.id)
                } ?: emptyList()
                adapter.setData(list)
            }
    }

    private fun showReviewMenu(review: Review) {
        AlertDialog.Builder(requireContext())
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> openEditScreen(review.id)
                    1 -> deleteReview(review.id)
                }
            }
            .show()
    }

    private fun openEditScreen(reviewId: String) {
        val action = ProfileFragmentDirections.actionProfileFragmentToNewPostFragment(reviewId)
        findNavController().navigate(action)
    }

    private fun deleteReview(id: String) {
        db.collection("reviews").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}