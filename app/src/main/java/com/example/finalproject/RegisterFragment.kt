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
import androidx.navigation.navOptions
import com.example.finalproject.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private var profilePhotoUri: Uri? = null

    private val pickProfilePhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                profilePhotoUri = it
                binding.ivProfilePreview.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.btnPickProfilePhoto.setOnClickListener {
            pickProfilePhoto.launch("image/*")
        }

        binding.btnRegister.setOnClickListener {
            val name     = binding.etFullName.text.toString().trim()
            val email    = binding.etRegEmail.text.toString().trim()
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
        val ref = FirebaseStorage.getInstance()
            .reference
            .child("profile_photos/$uid.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onComplete(downloadUri.toString())
                    }
                    .addOnFailureListener {
                        onComplete(null)
                    }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    private fun updateUserProfile(
        name: String,
        photoUrl: String?
    ) {
        val request = userProfileChangeRequest {
            displayName = name
            photoUrl?.let { this.photoUri = Uri.parse(it) }
        }

        FirebaseAuth.getInstance().currentUser!!
            .updateProfile(request)
            .addOnCompleteListener {
                findNavController().navigate(
                    R.id.restaurantListFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.nav_graph) { inclusive = true }
                    }
                )
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}