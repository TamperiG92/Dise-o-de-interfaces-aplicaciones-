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
import com.example.rodapp.databinding.FragmentMantenimientoBinding
import com.example.rodapp.models.MantenimientoInsert
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MantenimientoFragment : Fragment() {

    private var _binding: FragmentMantenimientoBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMantenimientoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tipos = resources.getStringArray(R.array.tipos_mantenimiento)
        binding.spinnerTipoMantenimiento.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, tipos
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.etFecha.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.label_seleccionar_fecha))
                .build()
            picker.show(parentFragmentManager, "dp_mant")
            picker.addOnPositiveButtonClickListener { millis ->
                binding.etFecha.setText(
                    SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(millis))
                )
            }
        }

        binding.btnClose.setOnClickListener { findNavController().navigateUp() }
        binding.btnGuardar.setOnClickListener { guardarMantenimiento() }
    }

    private fun guardarMantenimiento() {
        val motoId = sharedVm.motoId
        if (motoId == null) {
            toast(getString(R.string.error_inesperado))
            return
        }

        val tipo = binding.spinnerTipoMantenimiento.selectedItem?.toString() ?: ""
        val fechaDisplay = binding.etFecha.text.toString().trim()
        val kmStr = binding.etKmMantenimiento.text.toString().trim()
        val repetirStr = binding.etRepetir.text.toString().trim()
        val notas = binding.etNotas.text.toString().trim()

        if (fechaDisplay.isEmpty() || kmStr.isEmpty()) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val km = kmStr.toIntOrNull()
        if (km == null) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val isoFecha = try {
            val display = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            iso.format(display.parse(fechaDisplay)!!)
        } catch (_: Exception) {
            toast(getString(R.string.error_fecha_requerida))
            return
        }

        binding.btnGuardar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest.from("registros_mantenimiento").insert(
                    MantenimientoInsert(
                        moto_id = motoId,
                        tipo = tipo,
                        fecha = isoFecha,
                        kilometraje = km,
                        repetir_cada_km = repetirStr.toIntOrNull(),
                        notas = notas.ifEmpty { null }
                    )
                )
                toast(getString(R.string.mantenimiento_guardado))
                findNavController().navigateUp()
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
                binding.btnGuardar.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
