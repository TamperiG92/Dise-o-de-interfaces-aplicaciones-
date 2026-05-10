package com.example.rodapp.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegistroSOATFragment : Fragment() {

    private var _binding: FragmentRegistroSoatBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private var selectedFotoUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        selectedFotoUri = uri
        binding.imgPreviewSoat.setImageURI(uri)
        binding.imgPreviewSoat.clearColorFilter()
        binding.imgPreviewSoat.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
    }

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

        binding.etInicioSoat.setOnClickListener { mostrarDatePickerInicio() }
        binding.etVencimientoSoat.setOnClickListener { mostrarDatePicker(binding.etVencimientoSoat) }

        binding.cardFotoSoat.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnCloseSoat.setOnClickListener { findNavController().navigateUp() }
        binding.btnGuardarSoat.setOnClickListener { guardarSoat() }
    }

    private fun mostrarDatePickerInicio() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.label_seleccionar_fecha))
            .build()
        picker.show(parentFragmentManager, "dp_soat_inicio")
        picker.addOnPositiveButtonClickListener { millis ->
            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            binding.etInicioSoat.setText(fmt.format(Date(millis)))
            // Auto-calcular vencimiento = inicio + 1 año
            val cal = Calendar.getInstance().apply { timeInMillis = millis; add(Calendar.YEAR, 1) }
            binding.etVencimientoSoat.setText(fmt.format(cal.time))
        }
    }

    private fun mostrarDatePicker(campo: EditText) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.label_seleccionar_fecha))
            .build()
        picker.show(parentFragmentManager, "dp_soat_${campo.id}")
        picker.addOnPositiveButtonClickListener { millis ->
            campo.setText(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis)))
        }
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
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                var fotoUrl: String? = null
                if (userId != null) {
                    selectedFotoUri?.let { uri ->
                        try {
                            val bytes = requireContext().contentResolver
                                .openInputStream(uri)?.use { it.readBytes() }
                            if (bytes != null) {
                                val path = "$userId/soat/${System.currentTimeMillis()}.jpg"
                                SupabaseClient.client.storage.from("avatars").upload(path, bytes) { upsert = true }
                                fotoUrl = SupabaseClient.client.storage.from("avatars").publicUrl(path)
                            }
                        } catch (_: Exception) { }
                    }
                }

                SupabaseClient.client.postgrest.from("soat").insert(
                    SoatInsert(
                        moto_id = motoId,
                        numero_poliza = poliza,
                        aseguradora = aseguradora,
                        fecha_inicio = isoInicio,
                        fecha_vencimiento = isoVencimiento,
                        foto_url = fotoUrl
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

    private fun String.toIsoDate(): String? = try {
        val display = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        iso.format(display.parse(this)!!)
    } catch (_: Exception) { null }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
