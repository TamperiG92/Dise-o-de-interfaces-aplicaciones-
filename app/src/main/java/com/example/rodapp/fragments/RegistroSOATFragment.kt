package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentRegistroSoatBinding
import com.example.rodapp.models.SoatInsert
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistroSOATFragment : Fragment() {

    private var _binding: FragmentRegistroSoatBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroSoatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val aseguradoras = resources.getStringArray(R.array.aseguradoras_soat)
        binding.spinnerAseguradora.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, aseguradoras
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.etInicioSoat.setOnClickListener { mostrarDatePicker(binding.etInicioSoat) }
        binding.etVencimientoSoat.setOnClickListener { mostrarDatePicker(binding.etVencimientoSoat) }

        binding.btnCloseSoat.setOnClickListener { findNavController().navigateUp() }
        binding.btnGuardarSoat.setOnClickListener { guardarSoat() }
    }

    private fun guardarSoat() {
        val motoId = sharedVm.motoId
        if (motoId == null) {
            toast(getString(R.string.error_inesperado))
            return
        }

        val poliza = binding.etNPoliza.text.toString().trim()
        val aseguradora = binding.spinnerAseguradora.selectedItem?.toString() ?: ""
        val inicio = binding.etInicioSoat.text.toString().trim()
        val vencimiento = binding.etVencimientoSoat.text.toString().trim()

        if (poliza.isEmpty() || binding.spinnerAseguradora.selectedItemPosition == 0
            || inicio.isEmpty() || vencimiento.isEmpty()
        ) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val isoInicio = inicio.toIsoDate()
        val isoVencimiento = vencimiento.toIsoDate()
        if (isoInicio == null || isoVencimiento == null) {
            toast(getString(R.string.error_fecha_requerida))
            return
        }

        binding.btnGuardarSoat.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest.from("soat").insert(
                    SoatInsert(
                        moto_id = motoId,
                        numero_poliza = poliza,
                        aseguradora = aseguradora,
                        fecha_inicio = isoInicio,
                        fecha_vencimiento = isoVencimiento
                    )
                )
                toast(getString(R.string.soat_guardado))
                findNavController().navigateUp()
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
                binding.btnGuardarSoat.isEnabled = true
            }
        }
    }

    private fun mostrarDatePicker(campo: EditText) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.label_seleccionar_fecha))
            .build()
        picker.show(parentFragmentManager, "dp_soat_${campo.id}")
        picker.addOnPositiveButtonClickListener { millis ->
            campo.setText(SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(millis)))
        }
    }

    private fun String.toIsoDate(): String? = try {
        val display = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        iso.format(display.parse(this)!!)
    } catch (_: Exception) { null }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
