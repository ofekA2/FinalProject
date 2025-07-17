package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.finalproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        val user = FirebaseAuth.getInstance().currentUser

        user?.let {
            binding.tvProfileName.text = it.displayName ?: "Anonymous"

            binding.tvProfileEmail.text = it.email

            val photoUri = it.photoUrl
            if (photoUri != null) {
                Glide.with(this)
                    .load(photoUri)
                    .circleCrop()
                    .into(binding.ivProfilePic)
            } else {
                binding.ivProfilePic.setImageResource(R.drawable.ic_profile)
            }
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(
                R.id.loginFragment, null,
                navOptions = androidx.navigation.navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                }
            )
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}