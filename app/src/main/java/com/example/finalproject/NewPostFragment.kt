package com.example.finalproject

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.finalproject.databinding.FragmentNewPostBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private val args: NewPostFragmentArgs by navArgs()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var pickedImageUri: Uri? = null
    private var existingImageUrl: String? = null
    private var editingId: String? = null
    private var isEditMode = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pickedImageUri = uri
            if (uri != null) Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editingId = args.reviewId
        isEditMode = editingId != null

        if (isEditMode) {
            binding.btnSubmitPost.text = "Update"
            loadExistingReview(editingId!!)
        }

        binding.btnPickPostImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSubmitPost.setOnClickListener {
            if (!validateFields()) return@setOnClickListener

            val user = auth.currentUser ?: return@setOnClickListener
            val restaurant = binding.etRestaurant.text.toString().trim()
            val city = binding.etCity.text.toString().trim()
            val cuisine = binding.etCuisine.text.toString().trim()
            val rating = binding.sliderRating.value.toDouble()
            val priceTier = binding.sliderPrice.value.toInt()
            val reviewText = binding.etReviewText.text.toString().trim()

            binding.btnSubmitPost.isEnabled = false

            if (pickedImageUri != null) {
                uploadImage(user.uid, pickedImageUri!!) { url ->
                    if (isEditMode) {
                        updateReview(url ?: existingImageUrl, restaurant, city, cuisine, rating, priceTier, reviewText)
                    } else {
                        saveReview(url, user, restaurant, city, cuisine, rating, priceTier, reviewText)
                    }
                }
            } else {
                if (isEditMode) {
                    updateReview(existingImageUrl, restaurant, city, cuisine, rating, priceTier, reviewText)
                } else {
                    Toast.makeText(requireContext(), "Image is required", Toast.LENGTH_SHORT).show()
                    binding.btnSubmitPost.isEnabled = true
                }
            }
        }
    }

    private fun loadExistingReview(id: String) {
        db.collection("reviews").document(id).get()
            .addOnSuccessListener { snap ->
                val r = snap.toObject(Review::class.java) ?: return@addOnSuccessListener
                existingImageUrl = r.imageUrl
                binding.etRestaurant.setText(r.restaurant)
                binding.etCity.setText(r.city)
                binding.etCuisine.setText(r.cuisine)
                binding.sliderRating.value = r.rating.toFloat()
                binding.sliderPrice.value  = r.priceTier.toFloat()
                binding.etReviewText.setText(r.reviewText)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateFields(): Boolean {
        var ok = true
        if (binding.etRestaurant.text.toString().isBlank()) { binding.etRestaurant.error = "Required"; ok = false }
        if (binding.etCity.text.toString().isBlank())       { binding.etCity.error = "Required"; ok = false }
        if (binding.etCuisine.text.toString().isBlank())    { binding.etCuisine.error = "Required"; ok = false }
        if (binding.etReviewText.text.toString().isBlank()) { binding.etReviewText.error = "Required"; ok = false }
        return ok
    }

    private fun uploadImage(uid: String, uri: Uri, onDone: (String?) -> Unit) {
        val ref = FirebaseStorage.getInstance().reference
            .child("review_photos/${uid}_${UUID.randomUUID()}.jpg")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                ref.downloadUrl
            }
            .addOnSuccessListener { onDone(it.toString()) }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                onDone(null)
            }
    }

    private fun saveReview(
        imageUrl: String?,
        user: com.google.firebase.auth.FirebaseUser,
        restaurant: String,
        city: String,
        cuisine: String,
        rating: Double,
        priceTier: Int,
        reviewText: String
    ) {
        val review = hashMapOf(
            "restaurant" to restaurant,
            "city" to city,
            "cuisine" to cuisine,
            "rating" to rating,
            "priceTier" to priceTier,
            "reviewText" to reviewText,
            "imageUrl" to (imageUrl ?: ""),
            "timestamp" to Timestamp.now(),
            "authorId" to user.uid,
            "authorName" to (user.displayName ?: "Anonymous"),
            "authorPhoto" to (user.photoUrl?.toString().orEmpty())
        )

        db.collection("reviews").add(review)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Posted!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                binding.btnSubmitPost.isEnabled = true
            }
    }

    private fun updateReview(
        imageUrl: String?,
        restaurant: String,
        city: String,
        cuisine: String,
        rating: Double,
        priceTier: Int,
        reviewText: String
    ) {
        val map = mapOf(
            "restaurant" to restaurant,
            "city" to city,
            "cuisine" to cuisine,
            "rating" to rating,
            "priceTier" to priceTier,
            "reviewText" to reviewText,
            "imageUrl" to (imageUrl ?: "")
        )

        db.collection("reviews").document(editingId!!)
            .set(map, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                binding.btnSubmitPost.isEnabled = true
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}