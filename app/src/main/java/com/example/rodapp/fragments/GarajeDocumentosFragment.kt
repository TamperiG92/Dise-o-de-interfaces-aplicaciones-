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
import com.example.rodapp.databinding.FragmentGarajeDocumentosBinding
import com.example.rodapp.models.RtmRecord
import com.example.rodapp.models.SoatRecord
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class GarajeDocumentosFragment : Fragment() {

    private var _binding: FragmentGarajeDocumentosBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGarajeDocumentosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardSoat.setOnClickListener {
            findNavController().navigate(R.id.navigation_registro_soat)
        }
        binding.btnAddSoat.setOnClickListener {
            findNavController().navigate(R.id.navigation_registro_soat)
        }

        binding.cardRtm.setOnClickListener {
            findNavController().navigate(R.id.navigation_registro_rtm)
        }
        binding.btnAddRtm.setOnClickListener {
            findNavController().navigate(R.id.navigation_registro_rtm)
        }

        binding.cardOtroDoc.setOnClickListener {
            findNavController().navigate(R.id.navigation_documentos_adicionales)
        }
        binding.btnAddOtro.setOnClickListener {
            findNavController().navigate(R.id.navigation_documentos_adicionales)
        }

        binding.btnSettingsGaraje.setOnClickListener {
            findNavController().navigate(R.id.navigation_perfil)
        }

        binding.fabMisMotos.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch { cargarEstadoDocumentos() }
    }

    private suspend fun cargarEstadoDocumentos() {
        val motoId = sharedVm.motoId ?: run {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
            try {
                val motos = SupabaseClient.client.postgrest.from("motos")
                    .select { filter { eq("user_id", userId) }; limit(1L) }
                    .decodeList<com.example.rodapp.models.Moto>()
                if (motos.isNotEmpty()) {
                    val m = motos.first()
                    sharedVm.motoId = m.id
                    sharedVm.motoNombre = "${m.marca} ${m.modelo}"
                }
                sharedVm.motoId
            } catch (_: Exception) { null }
        } ?: return

        try {
            val soat = SupabaseClient.client.postgrest.from("soat")
                .select {
                    filter { eq("moto_id", motoId) }
                    order("fecha_vencimiento", Order.DESCENDING)
                    limit(1L)
                }
                .decodeList<SoatRecord>().firstOrNull()

            val rtm = SupabaseClient.client.postgrest.from("rtm")
                .select {
                    filter { eq("moto_id", motoId) }
                    order("fecha_vencimiento", Order.DESCENDING)
                    limit(1L)
                }
                .decodeList<RtmRecord>().firstOrNull()

            if (_binding == null) return

            val today = LocalDate.now()
            actualizarBadge(soat?.fecha_vencimiento, today, soat = true)
            actualizarBadge(rtm?.fecha_vencimiento, today, soat = false)
        } catch (_: Exception) { }
    }

    private fun actualizarBadge(fechaVenc: String?, today: LocalDate, soat: Boolean) {
        val badge = if (soat) binding.badgeEstadoSoat else binding.badgeEstadoRtm
        val txtVenc = if (soat) binding.txtVencSoat else binding.txtVencRtm
        val progressBar = if (soat) binding.progressSoat else binding.progressRtm
        val txtDias = if (soat) binding.txtDiasSoat else binding.txtDiasRtm

        if (fechaVenc == null) {
            badge.text = getString(R.string.label_sin_registro)
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            badge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.surface_stroke)
            txtVenc.text = if (soat) getString(R.string.label_soat_full) else getString(R.string.label_rtm_completo)
            progressBar.visibility = View.GONE
            txtDias.visibility = View.GONE
            return
        }

        val venc = runCatching { LocalDate.parse(fechaVenc) }.getOrNull()
        val vigente = venc != null && !venc.isBefore(today)

        if (vigente) {
            badge.text = getString(R.string.label_vigente)
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            badge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.surface_success)
        } else {
            badge.text = getString(R.string.label_vencido)
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_red))
            badge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.alert_red)
        }
        txtVenc.text = getString(R.string.label_vence_hasta, fechaVenc)

        if (venc != null) {
            val diasRestantes = ChronoUnit.DAYS.between(today, venc).toInt().coerceAtLeast(0)
            actualizarProgress(progressBar, txtDias, diasRestantes)
        }
    }

    private fun actualizarProgress(bar: LinearProgressIndicator, txtDias: android.widget.TextView, dias: Int) {
        bar.visibility = View.VISIBLE
        txtDias.visibility = View.VISIBLE

        val colorRes = when {
            dias > 90 -> R.color.cyan_primary
            dias > 30 -> R.color.warning_yellow
            else -> R.color.alert_red
        }
        val color = ContextCompat.getColor(requireContext(), colorRes)
        bar.setIndicatorColor(color)
        bar.setProgressCompat(dias.coerceAtMost(365), true)

        txtDias.text = when (dias) {
            0 -> getString(R.string.label_vencido)
            1 -> getString(R.string.label_un_dia_restante)
            else -> getString(R.string.label_dias_restantes, dias)
        }
        txtDias.setTextColor(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
