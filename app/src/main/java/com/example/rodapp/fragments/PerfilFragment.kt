package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.activities.main.MainActivity
import com.example.rodapp.databinding.FragmentPerfilBinding
import com.example.rodapp.models.UsuarioInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackPerfil.setOnClickListener { findNavController().navigateUp() }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_logout_title))
                .setMessage(getString(R.string.dialog_logout_message))
                .setPositiveButton(getString(R.string.btn_logout)) { _, _ ->
                    (requireActivity() as MainActivity).cerrarSesion()
                }
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show()
        }

        sharedVm.motoNombre?.let { binding.txtMotoPerfil.text = it }

        viewLifecycleOwner.lifecycleScope.launch { cargarUsuario() }
    }

    private suspend fun cargarUsuario() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        try {
            val usuarios = SupabaseClient.client.postgrest
                .from("users")
                .select {
                    filter { eq("id", userId) }
                    limit(1L)
                }
                .decodeList<UsuarioInfo>()

            val usuario = usuarios.firstOrNull() ?: return
            if (_binding == null) return
            binding.txtNombrePerfil.text = usuario.name
            if (sharedVm.motoNombre == null && usuario.name.isNotEmpty()) {
                // keep placeholder if no moto registered
            }
        } catch (_: Exception) { /* mantener placeholder */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
