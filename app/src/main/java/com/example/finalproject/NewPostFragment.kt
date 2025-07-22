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
import com.example.finalproject.databinding.FragmentNewPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlin.math.roundToInt

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private var imageUri:Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()){
            uri ->
        if (uri != null) {
            imageUri = uri
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)

        binding.btnPickPostImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSubmitPost.setOnClickListener { submit() }

        return binding.root
    }

    private fun submit() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return

        val restaurant = binding.etRestaurant.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val cuisine = binding.etCuisine.text.toString().trim()
        val rating = ((binding.sliderRating.value*10).roundToInt())/10.0
        val priceTier = binding.sliderPrice.value.toInt()
        val reviewText = binding.etReviewText.text.toString().trim()

        if (restaurant.isEmpty() || city.isEmpty() || reviewText.isEmpty() || cuisine.isEmpty() || imageUri==null) {
            Toast.makeText(context, "Please fill all of the fields", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun uploadImage(uid: String, uri: Uri, onDone: (String?) -> Unit) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = FirebaseStorage.getInstance().reference
            .child("review_images/$uid/$fileName")
        ref.putFile(uri)
            .continueWithTask { task -> if (!task.isSuccessful) task.exception?.let { throw it }; ref.downloadUrl }
            .addOnSuccessListener { onDone(it.toString()) }
            .addOnFailureListener { onDone(null) }
    }

    private fun saveReview(
        user: FirebaseUser,
        restaurant: String,
        city: String,
        cuisine: String,
        rating: Double,
        priceTier: Int,
        reviewText: String,
        imageUrl: String?
    ) {
        val data = hashMapOf(
            "restaurant" to restaurant,
            "city" to city,
            "cuisine" to cuisine,
            "rating" to rating,
            "priceTier" to priceTier,
            "reviewText" to reviewText,
            "imageUrl" to imageUrl,
            "timestamp" to FieldValue.serverTimestamp(),
            "authorId" to user.uid,
            "authorName" to (user.displayName ?: ""),
            "authorPhotoUrl" to (user.photoUrl?.toString() ?: "")
        )

        FirebaseFirestore.getInstance()
            .collection("reviews")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(context, "Posted!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}