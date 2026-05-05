package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentNuevoDocumentoBinding
import com.example.rodapp.models.DocumentoInsert
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NuevoDocumentoFragment : Fragment() {

    private var _binding: FragmentNuevoDocumentoBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevoDocumentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etVencimientoPersonal.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.label_seleccionar_fecha))
                .build()
            picker.show(parentFragmentManager, "dp_doc")
            picker.addOnPositiveButtonClickListener { millis ->
                binding.etVencimientoPersonal.setText(
                    SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(millis))
                )
            }
        }

        binding.btnBackNuevoDoc.setOnClickListener { findNavController().navigateUp() }
        binding.btnGuardarNuevoDoc.setOnClickListener { guardarDocumento() }
    }

    private fun guardarDocumento() {
        val motoId = sharedVm.motoId
        if (motoId == null) {
            toast(getString(R.string.error_inesperado))
            return
        }

        val nombre = binding.etNombreDoc.text.toString().trim()
        val entidad = binding.etEntidad.text.toString().trim()
        val vencimientoDisplay = binding.etVencimientoPersonal.text.toString().trim()
        val recordatorio = true

        if (nombre.isEmpty()) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val isoVencimiento = if (vencimientoDisplay.isNotEmpty()) {
            try {
                val display = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                iso.format(display.parse(vencimientoDisplay)!!)
            } catch (_: Exception) {
                toast(getString(R.string.error_fecha_requerida))
                return
            }
        } else null

        binding.btnGuardarNuevoDoc.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest.from("documentos").insert(
                    DocumentoInsert(
                        moto_id = motoId,
                        tipo = "PERSONALIZADO",
                        nombre = nombre,
                        entidad_emisora = entidad.ifEmpty { null },
                        fecha_vencimiento = isoVencimiento,
                        recordatorio_activo = recordatorio
                    )
                )
                toast(getString(R.string.documento_guardado))
                findNavController().navigateUp()
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
                binding.btnGuardarNuevoDoc.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
