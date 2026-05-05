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
import com.example.rodapp.databinding.FragmentGarajeBinding
import com.example.rodapp.models.Moto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class GarajeFragment : Fragment() {

    private var _binding: FragmentGarajeBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGarajeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegistrarMoto.setOnClickListener {
            findNavController().navigate(R.id.navigation_registro_moto)
        }

        viewLifecycleOwner.lifecycleScope.launch { cargarMoto() }
    }

    private suspend fun cargarMoto() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        try {
            val motos = SupabaseClient.client.postgrest
                .from("motos")
                .select {
                    filter { eq("user_id", userId) }
                    limit(1L)
                }
                .decodeList<Moto>()

            if (_binding == null) return
            if (motos.isNotEmpty()) {
                val moto = motos.first()
                sharedVm.motoId = moto.id
                sharedVm.motoNombre = "${moto.marca} ${moto.modelo}"
                binding.titleGarajeVacio.text = "${moto.marca} ${moto.modelo}"
                binding.subtitleGarajeVacio.text = getString(R.string.label_placa_formato, moto.placa)
                binding.btnRegistrarMoto.text = getString(R.string.btn_ver_documentos)
                binding.btnRegistrarMoto.setOnClickListener {
                    findNavController().navigate(R.id.navigation_garaje_documentos)
                }
            }
        } catch (_: Exception) { /* mantener empty state */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
