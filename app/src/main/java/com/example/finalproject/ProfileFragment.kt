package com.example.finalproject

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.finalproject.data.ReviewEntity
import com.example.finalproject.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var newPhotoUri: Uri? = null
    private var editDialogViews: EditViews? = null

    private lateinit var myPostsAdapter: ReviewAdapter
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var myPostsRegistration: ListenerRegistration? = null

    private var useDefaultPhoto: Boolean = false
    private var editAvatarPreview: ImageView? = null

    private val pickProfilePhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            useDefaultPhoto = false
            newPhotoUri = uri
            editAvatarPreview?.setImageURI(uri)
        } else {
            getContentFallbackProfile.launch("image/*")
        }
    }

    private val getContentFallbackProfile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            useDefaultPhoto = false
            newPhotoUri = uri
            editAvatarPreview?.setImageURI(uri)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser ?: return

        binding.tvProfileName.text = user.displayName ?: "Anonymous"
        user.photoUrl
            ?.let { Glide.with(this).load(it).circleCrop().into(binding.ivProfilePic) }
            ?: binding.ivProfilePic.setImageResource(R.drawable.ic_profile)

        binding.btnEditProfile.setOnClickListener { showEditDialog() }
        binding.btnLogout.setOnClickListener {
            auth.signOut()

            val intent = android.content.Intent(requireContext(), MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            requireActivity().finish()
        }

        myPostsAdapter = ReviewAdapter(emptyList())
        binding.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPosts.adapter = myPostsAdapter

        binding.fabAddPost.setOnClickListener {
            val action = ProfileFragmentDirections
                .actionProfileFragmentToNewPostFragment(null)
            findNavController().navigate(action)
        }

         myPostsAdapter.onMenuClick = { review, anchor -> showPopupMenu(anchor, review) }

         MyApp.database.reviewDao()
            .getByUser(user.uid)
            .asLiveData()
            .observe(viewLifecycleOwner) { entities ->
                val uiList = entities.map { ent ->
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
                        timestamp = com.google.firebase.Timestamp(
                            ent.timestampMs / 1000,
                            ((ent.timestampMs % 1000) * 1_000_000).toInt()
                        )
                    )
                }
                myPostsAdapter.setData(uiList)
            }
    }

    override fun onResume() {
        super.onResume()
        syncMyPostsFromFirestore()
    }

    private fun showPopupMenu(anchor: View, review: Review) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
        popup.inflate(R.menu.menu_review_item)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> { openEditScreen(review.id); true }
                R.id.action_delete -> { confirmDelete(review.id, review.imageUrl); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun openEditScreen(reviewId: String) {
        val action = ProfileFragmentDirections
            .actionProfileFragmentToNewPostFragment(reviewId)
        findNavController().navigate(action)
    }

    private fun confirmDelete(id: String, imageUrl: String?) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete")
            .setMessage("Delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("reviews").document(id)
                    .delete()
                    .addOnSuccessListener {
                        lifecycleScope.launch(Dispatchers.IO) {
                            MyApp.database.reviewDao().deleteById(id)
                        }
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("Profile", "Delete failed", e)
                        Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showEditDialog() {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val ivPreview = v.findViewById<ImageView>(R.id.ivPreview)
        val btnPick = v.findViewById<Button>(R.id.btnPick)
        val btnUseDef = v.findViewById<Button>(R.id.btnUseDefault)
        val etName = v.findViewById<TextInputEditText>(R.id.etName)
        val btnCancel = v.findViewById<Button>(R.id.btnCancel)
        val btnSave = v.findViewById<Button>(R.id.btnSave)

        val user = auth.currentUser!!
        etName.setText(user.displayName ?: "")
        user.photoUrl?.let { Glide.with(this).load(it).circleCrop().into(ivPreview) }
            ?: ivPreview.setImageResource(R.drawable.ic_profile)

        editAvatarPreview = ivPreview
        useDefaultPhoto = false
        newPhotoUri = null

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(v)
            .setCancelable(false)
            .create().apply { show() }

        editDialogViews = EditViews(ivPreview, etName, dialog)

        btnPick.setOnClickListener {
            pickProfilePhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnUseDef.setOnClickListener {
            useDefaultPhoto = true
            newPhotoUri = null
            ivPreview.setImageResource(R.drawable.ic_profile)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.text?.toString()?.trim().orEmpty()
            if (newName.isBlank()) {
                etName.error = "Name required"
                return@setOnClickListener
            }
            btnSave.isEnabled = false
            val uid = user.uid

            when {
                useDefaultPhoto -> {
                    updateUserProfile(newName, photoUrl = "")
                }
                newPhotoUri != null -> {
                    uploadProfilePhoto(uid, newPhotoUri!!) { url ->
                        if (url == null) {
                            Toast.makeText(requireContext(), "Photo upload failed", Toast.LENGTH_SHORT).show()
                            btnSave.isEnabled = true
                        } else {
                            updateUserProfile(newName, photoUrl = url)
                        }
                    }
                }
                else -> {
                    updateUserProfile(newName, photoUrl = null)
                }
            }
        }
    }

    private fun uploadProfilePhoto(uid: String, uri: Uri, onDone: (String?) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val url = CloudinaryUploader.upload(requireContext(), uri)
                onDone(url)
            }
            catch (e: Exception) {
                android.util.Log.e("Profile", "Cloudinary upload failed", e)
                Toast.makeText(requireContext(), "Photo upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                onDone(null)
            }
        }
    }

    private fun updateUserProfile(name: String, photoUrl: String?) {
        val user = auth.currentUser ?: return

        val updates = userProfileChangeRequest {
            displayName = name
            when {
                photoUrl == null -> {
                }
                photoUrl.isBlank() -> {
                    photoUri = null
                }
                else -> {
                    photoUri = Uri.parse(photoUrl)
                }
            }
        }

        user.updateProfile(updates).addOnCompleteListener { authTask ->
            if (!authTask.isSuccessful) {
                Toast.makeText(requireContext(), "Auth update failed", Toast.LENGTH_SHORT).show()
                editDialogViews?.dialog?.dismiss()
                newPhotoUri = null
                useDefaultPhoto = false
                editAvatarPreview = null
                editDialogViews = null
                return@addOnCompleteListener
            }

            val photoForDb = when {
                photoUrl == null -> user.photoUrl?.toString() ?: ""
                else -> photoUrl
            }

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "fullName" to name,
                        "photoUrl" to photoForDb
                    ),
                    SetOptions.merge()
                )
                .addOnCompleteListener {
                    binding.tvProfileName.text = name

                    if (photoForDb.isNullOrBlank()) {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_profile)
                    } else {
                        Glide.with(this).load(photoForDb).circleCrop().into(binding.ivProfilePic)
                    }

                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    editDialogViews?.dialog?.dismiss()

                    newPhotoUri = null
                    useDefaultPhoto = false
                    editAvatarPreview = null
                    editDialogViews = null
                }
        }
    }

    private fun syncMyPostsFromFirestore() {
        myPostsRegistration?.remove()

        val uid = auth.currentUser?.uid ?: return

        myPostsRegistration = db.collection("reviews")
            .whereEqualTo("authorId", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    val code = (error as? FirebaseFirestoreException)?.code
                    if (isAdded && auth.currentUser != null &&
                        code != FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {
                        Toast.makeText(requireContext(), "Listen failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val reviewEntities = snapshots.documents.map { doc ->
                        val r = doc.toSafeReview()
                        ReviewEntity(
                            id = r.id,
                            authorId = r.authorId,
                            authorName = r.authorName,
                            authorPhoto = r.authorPhoto,
                            restaurant = r.restaurant,
                            city = r.city,
                            cuisine = r.cuisine,
                            rating = r.rating,
                            priceTier = r.priceTier,
                            reviewText = r.reviewText,
                            imageUrl = r.imageUrl,
                            timestampMs = r.timestamp?.toDate()?.time ?: 0L
                        )
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        MyApp.database.reviewDao().upsert(reviewEntities)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        myPostsRegistration?.remove()
        myPostsRegistration = null
        _binding = null
    }

    private data class EditViews(
        val ivPreview: ImageView,
        val etName: TextInputEditText,
        val dialog: Dialog
    )
}