package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentDocumentosAdicionalesBinding
import com.example.rodapp.models.DocumentoAlerta
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.time.LocalDate

class DocumentosAdicionalesFragment : Fragment() {

    private var _binding: FragmentDocumentosAdicionalesBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentosAdicionalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackDocsAdic.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddLicencia.setOnClickListener {
            findNavController().navigate(
                R.id.navigation_nuevo_documento,
                Bundle().apply { putString("tipo_documento", "LICENCIA") }
            )
        }

        binding.btnAddTodoRiesgo.setOnClickListener {
            findNavController().navigate(
                R.id.navigation_nuevo_documento,
                Bundle().apply { putString("tipo_documento", "SEGURO_TODO_RIESGO") }
            )
        }

        binding.btnCrearDocPersonalizado.setOnClickListener {
            findNavController().navigate(
                R.id.navigation_nuevo_documento,
                Bundle().apply { putString("tipo_documento", "PERSONALIZADO") }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val motoId = sharedVm.motoId ?: return
        viewLifecycleOwner.lifecycleScope.launch { cargarEstadoDocumentos(motoId) }
    }

    private suspend fun cargarEstadoDocumentos(motoId: String) {
        try {
            val docs = SupabaseClient.client.postgrest.from("documentos")
                .select {
                    filter { eq("moto_id", motoId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<DocumentoAlerta>()

            if (_binding == null) return

            val licencia = docs.firstOrNull { it.tipo == "LICENCIA" }
            val seguro = docs.firstOrNull { it.tipo == "SEGURO_TODO_RIESGO" }

            val today = LocalDate.now()
            actualizarBadgeDocumento(licencia, today, binding.badgeLicencia)
            actualizarBadgeDocumento(seguro, today, binding.badgeSeguro)
        } catch (_: Exception) { }
    }

    private fun actualizarBadgeDocumento(doc: DocumentoAlerta?, today: LocalDate, badge: android.widget.TextView) {
        if (doc == null) {
            badge.visibility = View.GONE
            return
        }
        badge.visibility = View.VISIBLE
        val venc = doc.fecha_vencimiento?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (venc != null) {
            val vigente = !venc.isBefore(today)
            if (vigente) {
                badge.text = getString(R.string.label_vigente)
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
                badge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.surface_success)
            } else {
                badge.text = getString(R.string.label_vencido)
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                badge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.alert_red)
            }
        } else {
            badge.text = getString(R.string.label_registrada)
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            badge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.surface_success)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
