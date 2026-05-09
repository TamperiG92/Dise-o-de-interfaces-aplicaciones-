package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentCombustibleBinding
import com.example.rodapp.models.CombustibleInsert
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class CombustibleFragment : Fragment() {

    private var _binding: FragmentCombustibleBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private val tiposGasolinaDb = arrayOf("REGULAR", "PREMIUM", "DIESEL", "EXTRA")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCombustibleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tipos = resources.getStringArray(R.array.tipos_gasolina)
        binding.spinnerGasolina.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, tipos
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnRegistrar.setOnClickListener { registrarCombustible() }
    }

    private fun registrarCombustible() {
        val motoId = sharedVm.motoId
        if (motoId == null) {
            toast(getString(R.string.error_inesperado))
            return
        }

        val costoStr = binding.etCosto.text.toString().trim()
        val kmStr = binding.etKm.text.toString().trim()

        if (costoStr.isEmpty() || kmStr.isEmpty()) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val costo = costoStr.toDoubleOrNull()
        val km = kmStr.toIntOrNull()
        if (costo == null || km == null) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val litros = binding.etLitros.text.toString().trim().toDoubleOrNull()
        val tipo = tiposGasolinaDb[binding.spinnerGasolina.selectedItemPosition]

        binding.btnRegistrar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest.from("registros_combustible").insert(
                    CombustibleInsert(
                        moto_id = motoId,
                        tipo_gasolina = tipo,
                        costo = costo,
                        kilometraje = km,
                        litros = litros
                    )
                )
                toast(getString(R.string.combustible_registrado))
                findNavController().navigateUp()
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
                binding.btnRegistrar.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
