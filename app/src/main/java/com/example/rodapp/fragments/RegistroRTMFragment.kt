package com.example.rodapp.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.rodapp.databinding.FragmentRegistroRtmBinding
import com.example.rodapp.models.RtmInsert
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistroRTMFragment : Fragment() {

    private var _binding: FragmentRegistroRtmBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private var selectedFotoUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        selectedFotoUri = uri
        binding.cardFotoRtm.tag = uri
        // Show a visual indication that a photo was selected
        Toast.makeText(requireContext(), getString(R.string.label_adjunto_guardado), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroRtmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etExpedicionRtm.setOnClickListener { mostrarDatePicker(binding.etExpedicionRtm) }
        binding.etVencimientoRtm.setOnClickListener { mostrarDatePicker(binding.etVencimientoRtm) }

        binding.cardFotoRtm.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnBackRtm.setOnClickListener { findNavController().navigateUp() }
        binding.btnFinalizarRtm.setOnClickListener { guardarRtm() }
    }

    private fun guardarRtm() {
        val motoId = sharedVm.motoId
        if (motoId == null) {
            toast(getString(R.string.error_inesperado))
            return
        }

        val certificado = binding.etNCertificado.text.toString().trim()
        val cda = binding.etNombreCda.text.toString().trim()
        val expedicion = binding.etExpedicionRtm.text.toString().trim()
        val vencimiento = binding.etVencimientoRtm.text.toString().trim()

        if (certificado.isEmpty() || cda.isEmpty() || expedicion.isEmpty() || vencimiento.isEmpty()) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val isoExpedicion = expedicion.toIsoDate()
        val isoVencimiento = vencimiento.toIsoDate()
        if (isoExpedicion == null || isoVencimiento == null) {
            toast(getString(R.string.error_fecha_requerida))
            return
        }

        binding.btnFinalizarRtm.isEnabled = false

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
                                val path = "$userId/rtm/${System.currentTimeMillis()}.jpg"
                                SupabaseClient.client.storage.from("avatars").upload(path, bytes) { upsert = true }
                                fotoUrl = SupabaseClient.client.storage.from("avatars").publicUrl(path)
                            }
                        } catch (_: Exception) { }
                    }
                }

                SupabaseClient.client.postgrest.from("rtm").insert(
                    RtmInsert(
                        moto_id = motoId,
                        numero_certificado = certificado,
                        nombre_cda = cda,
                        fecha_expedicion = isoExpedicion,
                        fecha_vencimiento = isoVencimiento,
                        foto_url = fotoUrl
                    )
                )
                toast(getString(R.string.rtm_guardada))
                findNavController().navigateUp()
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
                binding.btnFinalizarRtm.isEnabled = true
            }
        }
    }

    private fun mostrarDatePicker(campo: EditText) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.label_seleccionar_fecha))
            .build()
        picker.show(parentFragmentManager, "dp_rtm_${campo.id}")
        picker.addOnPositiveButtonClickListener { millis ->
            campo.setText(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis)))
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
