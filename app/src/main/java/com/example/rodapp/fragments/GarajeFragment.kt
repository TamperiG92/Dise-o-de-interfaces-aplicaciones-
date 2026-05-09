package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
        binding.rvMotos.layoutManager = LinearLayoutManager(requireContext())
        binding.btnRegistrarMoto.setOnClickListener {
            findNavController().navigate(R.id.navigation_registro_moto)
        }
        binding.fabAgregarMoto.setOnClickListener {
            findNavController().navigate(R.id.navigation_registro_moto)
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch { cargarMotos() }
    }

    private suspend fun cargarMotos() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        try {
            val motos = SupabaseClient.client.postgrest
                .from("motos")
                .select { filter { eq("user_id", userId) } }
                .decodeList<Moto>()

            if (_binding == null) return

            if (motos.isEmpty()) {
                binding.layoutGarajeVacio.visibility = View.VISIBLE
                binding.layoutGarajeLista.visibility = View.GONE
            } else {
                binding.layoutGarajeVacio.visibility = View.GONE
                binding.layoutGarajeLista.visibility = View.VISIBLE
                binding.rvMotos.adapter = MotoAdapter(motos) { moto ->
                    sharedVm.motoId = moto.id
                    sharedVm.motoNombre = "${moto.marca} ${moto.modelo}"
                    findNavController().navigate(R.id.navigation_garaje_documentos)
                }
                if (sharedVm.motoId == null && motos.size == 1) {
                    sharedVm.motoId = motos[0].id
                    sharedVm.motoNombre = "${motos[0].marca} ${motos[0].modelo}"
                    findNavController().navigate(R.id.navigation_garaje_documentos)
                }
            }
        } catch (_: Exception) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
