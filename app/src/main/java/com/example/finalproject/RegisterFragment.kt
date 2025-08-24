package com.example.finalproject

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.finalproject.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private var profilePhotoUri: Uri? = null

    private val openDocRegister = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }

            profilePhotoUri = uri
            binding.ivProfilePreview.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.btnPickProfilePhoto.setOnClickListener {
            openDocRegister.launch(arrayOf("image/*"))
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etRegEmail.text.toString().trim()
            val password = binding.etRegPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(
                    requireContext(),
                    "Name, valid email & 6+ char password required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            binding.btnRegister.isEnabled = false

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user!!

                    if (profilePhotoUri != null) {
                        uploadProfilePhoto(user.uid, profilePhotoUri!!) { downloadUrl ->
                            updateUserProfile(name, downloadUrl)
                        }
                    } else {
                        updateUserProfile(name, null)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Registration failed: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnRegister.isEnabled = true
                }
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        return binding.root
    }

    private fun uploadProfilePhoto(
        uid: String,
        uri: Uri,
        onComplete: (downloadUrl: String?) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val url = CloudinaryUploader.upload(requireContext(), uri)
                onComplete(url)
            } catch (e: Exception) {
                Log.e("Register", "Avatar upload failed", e)
                Toast.makeText(requireContext(), "Avatar upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                onComplete(null)
            }
        }
    }

    private fun updateUserProfile(
        name: String,
        photoUrl: String?
    ) {
        val request = userProfileChangeRequest {
            displayName = name
            if (!photoUrl.isNullOrBlank()) {
                photoUri = Uri.parse(photoUrl)
            }
        }

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser!!

        user.updateProfile(request).addOnCompleteListener {
            val finalPhoto = photoUrl ?: ""
            val data = hashMapOf(
                "uid" to user.uid,
                "name" to name,
                "email" to (user.email ?: ""),
                "photoUrl" to finalPhoto
            )

            FirebaseFirestore.getInstance()
                .collection("users").document(user.uid)
                .set(data)
                .addOnCompleteListener {
                    findNavController().navigate(
                        R.id.restaurantListFragment,
                        null,
                        navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
                    )
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}