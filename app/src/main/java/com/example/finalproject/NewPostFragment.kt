package com.example.finalproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
    private var editingId: String? = null
    private var isEditMode = false
    private var existingImageUrl: String? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var pickedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pickedImageUri = uri
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                binding.ivPostPreview.setImageURI(uri)
                binding.ivPostPreview.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentNewPostBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editingId  = args.reviewId
        isEditMode = editingId != null

        if (isEditMode) {
            binding.btnSubmitPost.text = "Update"
            loadExistingReview(editingId!!)
        }

        binding.btnPickPostImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnSubmitPost.setOnClickListener {
            if (!validateFields()) return@setOnClickListener

            val user = auth.currentUser ?: return@setOnClickListener
            val restaurant = binding.etRestaurant.text.toString().trim()
            val city = binding.etCity.text.toString().trim()
            val cuisine = binding.etCuisine .text.toString().trim()
            val rating = binding.sliderRating.value.toDouble()
            val priceTier = binding.sliderPrice.value.toInt()
            val reviewText = binding.etReviewText.text.toString().trim()

            binding.btnSubmitPost.isEnabled = false

            fun doUpdate(imageUrl: String?) {
                val data = mapOf(
                    "authorId" to user.uid,
                    "authorName" to user.displayName,
                    "authorPhoto" to user.photoUrl?.toString(),
                    "restaurant" to restaurant,
                    "city" to city,
                    "cuisine" to cuisine,
                    "rating" to rating,
                    "priceTier" to priceTier,
                    "reviewText" to reviewText,
                    "imageUrl" to (imageUrl ?: ""),
                    "timestamp" to Timestamp.now()
                )
                val docRef = if (isEditMode && editingId != null) {
                    db.collection("reviews").document(editingId!!)
                } else {
                    db.collection("reviews").document()
                }

                docRef.set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Post updated!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        binding.btnSubmitPost.isEnabled = true
                    }
            }
            if (pickedImageUri != null) {
                uploadImage(user.uid, pickedImageUri!!) { url ->
                    if (url != null) {
                        doUpdate(url)
                    } else {
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                        binding.btnSubmitPost.isEnabled = true
                    }
                }
            } else if (isEditMode) {
                doUpdate(existingImageUrl)
            } else {
                Toast.makeText(requireContext(), "Image is required", Toast.LENGTH_SHORT).show()
                binding.btnSubmitPost.isEnabled = true
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
                binding.etCuisine  .setText(r.cuisine)
                binding.sliderRating.value = r.rating.toFloat()
                binding.sliderPrice .value = r.priceTier.toFloat()
                binding.etReviewText.setText(r.reviewText)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateFields(): Boolean {
        var ok = true

        val restaurant = binding.etRestaurant.text.toString().trim()
        if (restaurant.isBlank()) {
            binding.etRestaurant.error = "Required"
            ok = false
        }

        val city = binding.etCity.text.toString().trim()
        if (city.isBlank()) {
            binding.etCity.error = "Required"
            ok = false
        }

        val cuisine = binding.etCuisine.text.toString().trim()
        if (cuisine.isBlank()) {
            binding.etCuisine.error = "Required"
            ok = false
        }

        val reviewText = binding.etReviewText.text.toString().trim()
        if (reviewText.isBlank()) {
            binding.etReviewText.error = "Required"
            ok = false
        }

        return ok
    }

    private fun uploadImage(uid: String, uri: Uri, onDone: (String?) -> Unit) {
        val ref = FirebaseStorage.getInstance()
            .reference.child("review_photos/${uid}_${UUID.randomUUID()}.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    onDone(url.toString())
                }.addOnFailureListener {
                    Log.e("NewPost", "Download URL fetch failed", it)
                    onDone(null)
                }
            }
            .addOnFailureListener {
                Log.e("NewPost", "Image upload failed", it)
                Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                onDone(null)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}