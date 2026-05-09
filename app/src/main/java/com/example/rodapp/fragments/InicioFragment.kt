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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentInicioBinding
import com.example.rodapp.models.HistorialItem
import com.example.rodapp.models.KmRecord
import com.example.rodapp.models.Moto
import com.example.rodapp.models.MotoOdo
import com.example.rodapp.models.RtmRecord
import com.example.rodapp.models.SoatRecord
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.time.LocalDate

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private var isMenuOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnToggleMockup.visibility = View.GONE

        binding.fabMain.setOnClickListener { toggleMenu() }

        val irCombustible = {
            findNavController().navigate(R.id.navigation_combustible)
            toggleMenu()
        }
        val irMantenimiento = {
            findNavController().navigate(R.id.navigation_mantenimiento)
            toggleMenu()
        }

        binding.btnNuevoCombustible.setOnClickListener { irCombustible() }
        binding.fabCombustibleBtn.setOnClickListener { irCombustible() }

        binding.btnNuevoMantenimiento.setOnClickListener { irMantenimiento() }
        binding.fabMantenimientoBtn.setOnClickListener { irMantenimiento() }

        binding.llenoContent.txtVerHistorial.setOnClickListener {
            findNavController().navigate(R.id.navigation_historial)
        }

        binding.llenoContent.rvActividadReciente.layoutManager =
            LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch { cargarDashboard() }
    }

    private suspend fun cargarDashboard() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return

        val motoId = sharedVm.motoId ?: run {
            try {
                val motos = SupabaseClient.client.postgrest.from("motos")
                    .select { filter { eq("user_id", userId) }; limit(1L) }
                    .decodeList<Moto>()
                if (motos.isNotEmpty()) {
                    val m = motos.first()
                    sharedVm.motoId = m.id
                    sharedVm.motoNombre = "${m.marca} ${m.modelo}"
                }
            } catch (_: Exception) { }
            sharedVm.motoId
        }

        if (_binding == null) return

        if (motoId == null) {
            binding.layoutVacio.visibility = View.VISIBLE
            binding.layoutLleno.visibility = View.GONE
            return
        }

        binding.layoutVacio.visibility = View.GONE
        binding.layoutLleno.visibility = View.VISIBLE

        val lleno = binding.llenoContent
        sharedVm.motoNombre?.let { lleno.txtMotoNombreInicio.text = it }

        try {
            val odo = SupabaseClient.client.postgrest.from("motos")
                .select { filter { eq("id", motoId) }; limit(1L) }
                .decodeList<MotoOdo>().firstOrNull()

            val ultimoKm = SupabaseClient.client.postgrest.from("registros_combustible")
                .select { filter { eq("moto_id", motoId) }; order("kilometraje", Order.DESCENDING); limit(1L) }
                .decodeList<KmRecord>().firstOrNull()

            val km = ultimoKm?.kilometraje ?: odo?.odometro_inicial ?: 0
            lleno.tvKm.text = getString(R.string.label_km_con_valor, km)

            val today = LocalDate.now()

            val soat = SupabaseClient.client.postgrest.from("soat")
                .select { filter { eq("moto_id", motoId) }; order("fecha_vencimiento", Order.DESCENDING); limit(1L) }
                .decodeList<SoatRecord>().firstOrNull()

            val rtm = SupabaseClient.client.postgrest.from("rtm")
                .select { filter { eq("moto_id", motoId) }; order("fecha_vencimiento", Order.DESCENDING); limit(1L) }
                .decodeList<RtmRecord>().firstOrNull()

            if (_binding == null) return

            actualizarEstado(soat?.fecha_vencimiento, today, lleno.txtSoatEstado, lleno.dotSoatEstado)
            actualizarEstado(rtm?.fecha_vencimiento, today, lleno.txtRtmEstado, lleno.dotRtmEstado)
        } catch (_: Exception) { }

        cargarActividadReciente(motoId)
    }

    private suspend fun cargarActividadReciente(motoId: String) {
        try {
            val items = SupabaseClient.client.postgrest.from("v_historial")
                .select {
                    filter { eq("moto_id", motoId) }
                    order("created_at", Order.DESCENDING)
                    limit(5L)
                }
                .decodeList<HistorialItem>()

            if (_binding == null) return
            val lleno = binding.llenoContent

            if (items.isEmpty()) {
                lleno.rvActividadReciente.visibility = View.GONE
                lleno.txtSinActividadInicio.visibility = View.VISIBLE
            } else {
                lleno.rvActividadReciente.adapter = HistorialAdapter(items)
                lleno.rvActividadReciente.visibility = View.VISIBLE
                lleno.txtSinActividadInicio.visibility = View.GONE
            }
        } catch (_: Exception) { }
    }

    private fun actualizarEstado(
        fechaVenc: String?,
        today: LocalDate,
        txtEstado: android.widget.TextView,
        dot: android.view.View
    ) {
        if (fechaVenc == null) {
            txtEstado.text = getString(R.string.label_sin_registro)
            txtEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            dot.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.text_gray)
            return
        }
        val venc = runCatching { LocalDate.parse(fechaVenc) }.getOrNull()
        val vigente = venc != null && !venc.isBefore(today)
        if (vigente) {
            txtEstado.text = getString(R.string.label_vigente)
            txtEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            dot.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.success_green)
        } else {
            txtEstado.text = getString(R.string.label_vencido)
            txtEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_red))
            dot.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.alert_red)
        }
    }

    private fun toggleMenu() {
        if (!isMenuOpen) {
            binding.fabMenu.visibility = View.VISIBLE
            binding.fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            isMenuOpen = true
        } else {
            binding.fabMenu.visibility = View.GONE
            binding.fabMain.setImageResource(android.R.drawable.ic_input_add)
            isMenuOpen = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
