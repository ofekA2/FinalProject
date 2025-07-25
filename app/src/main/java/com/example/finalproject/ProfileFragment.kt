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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.finalproject.data.AppDatabase
import com.example.finalproject.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var newPhotoUri: Uri? = null
    private var editDialogViews: EditViews? = null

    private lateinit var myPostsAdapter: ReviewAdapter
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth:FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newPhotoUri = uri
        uri?.let { editDialogViews?.ivPreview?.setImageURI(it) }
    }

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
        val p = user.photoUrl
        if (p != null) {
            Glide.with(this).load(p).circleCrop().into(binding.ivProfilePic)
        } else {
            binding.ivProfilePic.setImageResource(R.drawable.ic_profile)
        }

        binding.btnEditProfile.setOnClickListener { showEditDialog() }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(
                R.id.loginFragment, null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                }
            )
        }

        myPostsAdapter = ReviewAdapter(emptyList())
        binding.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPosts.adapter = myPostsAdapter

        binding.fabAddPost.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToNewPostFragment(null)
            findNavController().navigate(action)
        }

        myPostsAdapter.onMenuClick = { review, anchor -> showPopupMenu(anchor, review) }

        loadMyPosts(user.uid)
    }

    private fun loadMyPosts(uid: String) {
        db.collection("reviews")
            .whereEqualTo("authorId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val list:List<Review> = snapshot?.documents?.map { doc ->
                    doc.toObject(Review::class.java)!!.copy(id = doc.id)
                } ?: emptyList()

                myPostsAdapter.setData(list)
            }
    }

    private fun showPopupMenu(anchor:View, review: Review) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
        popup.inflate(R.menu.menu_review_item)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    openEditScreen(review.id)
                    true
                }
                R.id.action_delete -> {
                    confirmDelete(review.id, review.imageUrl)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openEditScreen(reviewId: String) {
        val action = ProfileFragmentDirections.actionProfileFragmentToNewPostFragment(reviewId)
        findNavController().navigate(action)
    }

    private fun confirmDelete(id: String, imageUrl: String?) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("Delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                FirebaseFirestore.getInstance()
                    .collection("reviews")
                    .document(id)
                    .delete()
                    .addOnSuccessListener {
                        // then delete local Room copy
                        lifecycleScope.launch {
                            MyApp.database.reviewDao().deleteById(id)
                        }
                        imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            FirebaseStorage.getInstance().getReferenceFromUrl(url).delete()
                        }
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog() {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile, null)

        val ivPreview = v.findViewById<ImageView>(R.id.ivPreview)
        val btnPick = v.findViewById<Button>(R.id.btnPick)
        val etName = v.findViewById<TextInputEditText>(R.id.etName)
        val btnCancel = v.findViewById<Button>(R.id.btnCancel)
        val btnSave = v.findViewById<Button>(R.id.btnSave)

        val user = auth.currentUser!!
        etName.setText(user.displayName ?: "")
        user.photoUrl?.let { Glide.with(this).load(it).circleCrop().into(ivPreview) }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(v)
            .setCancelable(false)
            .create()
        dialog.show()

        editDialogViews = EditViews(ivPreview, etName, dialog)

        btnPick.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.text?.toString()?.trim().orEmpty()
            if (newName.isBlank()) {
                etName.error = "Name required"
                return@setOnClickListener
            }
            btnSave.isEnabled = false

            val uri = newPhotoUri
            if (uri != null) {
                uploadProfilePhoto(user.uid, uri) { url ->
                    updateUserProfile(newName, url)
                }
            } else {
                updateUserProfile(newName, null)
            }
        }
    }

    private fun uploadProfilePhoto(uid: String, uri: Uri, onDone: (String?) -> Unit) {
        val ref = FirebaseStorage.getInstance().reference
            .child("profile_photos/$uid.jpg")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                ref.downloadUrl
            }
            .addOnSuccessListener { onDone(it.toString()) }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Photo upload failed", Toast.LENGTH_SHORT).show()
                onDone(null)
            }
    }

    private fun updateUserProfile(name: String, photoUrl: String?) {
        val user = auth.currentUser ?: return
        val updates = userProfileChangeRequest {
            displayName = name
            if (photoUrl != null) photoUri = Uri.parse(photoUrl)
        }

        user.updateProfile(updates).addOnCompleteListener { authTask ->
            if (!authTask.isSuccessful) {
                Toast.makeText(requireContext(), "Auth update failed", Toast.LENGTH_SHORT).show()
                editDialogViews?.dialog?.dismiss()
                return@addOnCompleteListener
            }

            FirebaseFirestore.getInstance().collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "fullName" to name,
                        "photoUrl" to (photoUrl ?: user.photoUrl?.toString().orEmpty())
                    ),
                    SetOptions.merge()
                )
                .addOnCompleteListener {
                    binding.tvProfileName.text = name
                    if (photoUrl != null) {
                        Glide.with(this).load(photoUrl).circleCrop().into(binding.ivProfilePic)
                    }
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    editDialogViews?.dialog?.dismiss()
                    newPhotoUri = null
                    editDialogViews = null
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class EditViews(
        val ivPreview: ImageView,
        val etName: TextInputEditText,
        val dialog: Dialog
    )
}