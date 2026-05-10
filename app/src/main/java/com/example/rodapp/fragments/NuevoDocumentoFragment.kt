package com.example.rodapp.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import com.example.rodapp.databinding.FragmentNuevoDocumentoBinding
import com.example.rodapp.models.DocumentoInsert
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NuevoDocumentoFragment : Fragment() {

    private var _binding: FragmentNuevoDocumentoBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private var tipoDocumento: String = "PERSONALIZADO"
    private var selectedAdjuntoUri: Uri? = null

    private val pickAdjunto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        selectedAdjuntoUri = uri
        toast(getString(R.string.label_adjunto_guardado))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevoDocumentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tipoDocumento = arguments?.getString("tipo_documento") ?: "PERSONALIZADO"

        configurarPorTipo()

        binding.etVencimientoPersonal.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.label_seleccionar_fecha))
                .build()
            picker.show(parentFragmentManager, "dp_doc")
            picker.addOnPositiveButtonClickListener { millis ->
                binding.etVencimientoPersonal.setText(
                    SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis))
                )
            }
        }

        binding.cardAdjunto.setOnClickListener {
            pickAdjunto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnBackNuevoDoc.setOnClickListener { findNavController().navigateUp() }
        binding.btnGuardarNuevoDoc.setOnClickListener { guardarDocumento() }
    }

    private fun configurarPorTipo() {
        when (tipoDocumento) {
            "LICENCIA" -> {
                binding.titleNuevoDoc.text = getString(R.string.label_licencia_conduccion)
                // Ocultar nombre (se auto-generará desde tipo de licencia)
                binding.lblNombreDoc.visibility = View.GONE
                binding.etNombreDoc.visibility = View.GONE
                // Ocultar entidad emisora (siempre Mintransporte)
                binding.lblEntidad.visibility = View.GONE
                binding.etEntidad.visibility = View.GONE
                // Mostrar spinner A1/A2
                binding.lblTipoLicencia.visibility = View.VISIBLE
                binding.spinnerTipoLicencia.visibility = View.VISIBLE
                val tiposLic = resources.getStringArray(R.array.tipos_licencia)
                binding.spinnerTipoLicencia.adapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_spinner_item, tiposLic
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
            "SEGURO_TODO_RIESGO" -> {
                binding.titleNuevoDoc.text = getString(R.string.label_seguro_todo_riesgo)
                binding.etNombreDoc.setText(getString(R.string.label_seguro_todo_riesgo))
                binding.etNombreDoc.isEnabled = false
                binding.lblNombreDoc.visibility = View.GONE
                binding.etNombreDoc.visibility = View.GONE
                // Entidad emisora visible con hint de aseguradora
                binding.etEntidad.hint = getString(R.string.hint_aseguradora_seguro)
            }
        }
    }

    private fun guardarDocumento() {
        val motoId = sharedVm.motoId
        if (motoId == null) {
            toast(getString(R.string.error_inesperado))
            return
        }

        val nombre = when (tipoDocumento) {
            "LICENCIA" -> {
                val tipoSel = binding.spinnerTipoLicencia.selectedItem?.toString() ?: "A1"
                "Licencia $tipoSel"
            }
            "SEGURO_TODO_RIESGO" -> getString(R.string.label_seguro_todo_riesgo)
            else -> binding.etNombreDoc.text.toString().trim()
        }

        if (nombre.isEmpty()) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val entidad = when (tipoDocumento) {
            "LICENCIA" -> null
            else -> binding.etEntidad.text.toString().trim().ifEmpty { null }
        }

        val vencimientoDisplay = binding.etVencimientoPersonal.text.toString().trim()
        val isoVencimiento = if (vencimientoDisplay.isNotEmpty()) {
            try {
                val display = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                iso.format(display.parse(vencimientoDisplay)!!)
            } catch (_: Exception) {
                toast(getString(R.string.error_fecha_requerida))
                return
            }
        } else null

        val recordatorioActivo = binding.switchRecordatorio.isChecked

        binding.btnGuardarNuevoDoc.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                var archivoUrl: String? = null
                if (userId != null) {
                    selectedAdjuntoUri?.let { uri ->
                        try {
                            val bytes = requireContext().contentResolver
                                .openInputStream(uri)?.use { it.readBytes() }
                            if (bytes != null) {
                                val ext = requireContext().contentResolver
                                    .getType(uri)?.let { if (it.contains("pdf")) "pdf" else "jpg" } ?: "jpg"
                                val path = "$userId/docs/${System.currentTimeMillis()}.$ext"
                                SupabaseClient.client.storage.from("avatars").upload(path, bytes) { upsert = true }
                                archivoUrl = SupabaseClient.client.storage.from("avatars").publicUrl(path)
                            }
                        } catch (_: Exception) { }
                    }
                }

                SupabaseClient.client.postgrest.from("documentos").insert(
                    DocumentoInsert(
                        moto_id = motoId,
                        tipo = tipoDocumento,
                        nombre = nombre,
                        entidad_emisora = entidad,
                        fecha_vencimiento = isoVencimiento,
                        recordatorio_activo = recordatorioActivo,
                        archivo_url = archivoUrl
                    )
                )

                // Abrir Google Calendar si recordatorio está activo y hay fecha de vencimiento
                if (recordatorioActivo && isoVencimiento != null) {
                    abrirGoogleCalendar(nombre, isoVencimiento)
                }

                toast(getString(R.string.documento_guardado))
                findNavController().navigateUp()
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
                binding.btnGuardarNuevoDoc.isEnabled = true
            }
        }
    }

    private fun abrirGoogleCalendar(nombreDoc: String, isoFecha: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("America/Bogota")
            }
            val date = sdf.parse(isoFecha) ?: return
            val placa = sharedVm.motoNombre ?: ""

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, getString(R.string.label_calendario_recordatorio, nombreDoc))
                putExtra(CalendarContract.Events.DESCRIPTION, "Vencimiento de $nombreDoc${if (placa.isNotEmpty()) " - $placa" else ""}")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.time)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, date.time + 60 * 60 * 1000)
                putExtra(CalendarContract.Events.ALL_DAY, false)
                putExtra(CalendarContract.Events.HAS_ALARM, 1)
            }
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
                toast(getString(R.string.label_recordatorio_creado))
            }
        } catch (_: Exception) { }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
