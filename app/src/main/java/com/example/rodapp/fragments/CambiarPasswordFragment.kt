package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentCambiarPasswordBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class CambiarPasswordFragment : Fragment() {

    private var _binding: FragmentCambiarPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCambiarPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBackPass.setOnClickListener { findNavController().navigateUp() }
        binding.btnCambiarPassword.setOnClickListener { cambiarPassword() }
    }

    private fun cambiarPassword() {
        val nueva = binding.etNuevaPassword.text.toString()
        val confirmar = binding.etConfirmarPassword.text.toString()

        if (nueva.length < 8) {
            toast(getString(R.string.error_pass_corta))
            return
        }
        if (nueva != confirmar) {
            toast(getString(R.string.error_passwords_no_coinciden))
            return
        }

        binding.btnCambiarPassword.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.updateUser {
                    password = nueva
                }
                toast(getString(R.string.password_actualizada))
                findNavController().navigateUp()
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
                binding.btnCambiarPassword.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
