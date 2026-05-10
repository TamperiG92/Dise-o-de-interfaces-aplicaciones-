package com.example.rodapp.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.rodapp.models.AceiteRecord
import com.example.rodapp.models.HistorialItem
import com.example.rodapp.models.KmRecord
import com.example.rodapp.models.KmRutaRecord
import com.example.rodapp.models.Moto
import com.example.rodapp.models.MotoOdo
import com.example.rodapp.models.RtmRecord
import com.example.rodapp.models.RutaInsert
import com.example.rodapp.models.SoatRecord
import com.example.rodapp.services.RodandoEstado
import com.example.rodapp.services.RodandoService
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
    private var kmActualMoto = 0

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) iniciarRuta()
    }

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

        binding.llenoContent.btnIniciarRuta.setOnClickListener {
            if (hasLocationPermission()) iniciarRuta()
            else locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.llenoContent.btnFinalizarRuta.setOnClickListener { finalizarRuta() }

        observarEstadoRuta()
    }

    private fun observarEstadoRuta() {
        viewLifecycleOwner.lifecycleScope.launch {
            RodandoEstado.activa.collect { activa ->
                if (_binding == null) return@collect
                val lleno = binding.llenoContent
                lleno.btnIniciarRuta.visibility = if (activa) View.GONE else View.VISIBLE
                lleno.cardEnRuta.visibility = if (activa) View.VISIBLE else View.GONE
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            RodandoEstado.distanciaMetros.collect { metros ->
                if (_binding == null) return@collect
                val kmStr = "%.1f".format(metros / 1000f)
                binding.llenoContent.txtKmRuta.text =
                    getString(R.string.label_km_recorridos, kmStr)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            RodandoEstado.tiempoSegundos.collect { secs ->
                if (_binding == null) return@collect
                binding.llenoContent.txtTimerRuta.text = formatTiempo(secs)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            binding.fabMain.visibility = View.VISIBLE
            binding.fabMenu.visibility = View.GONE
            binding.fabMain.setImageResource(android.R.drawable.ic_input_add)
            isMenuOpen = false
        }
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

        var km = 0
        try {
            val odo = SupabaseClient.client.postgrest.from("motos")
                .select { filter { eq("id", motoId) }; limit(1L) }
                .decodeList<MotoOdo>().firstOrNull()

            val ultimoKmComb = SupabaseClient.client.postgrest.from("registros_combustible")
                .select { filter { eq("moto_id", motoId) }; order("kilometraje", Order.DESCENDING); limit(1L) }
                .decodeList<KmRecord>().firstOrNull()

            val ultimoKmRuta = SupabaseClient.client.postgrest.from("registros_ruta")
                .select { filter { eq("moto_id", motoId) }; order("km_fin", Order.DESCENDING); limit(1L) }
                .decodeList<KmRutaRecord>().firstOrNull()

            km = maxOf(
                ultimoKmComb?.kilometraje ?: 0,
                ultimoKmRuta?.km_fin ?: 0,
                odo?.odometro_inicial ?: 0
            )
            kmActualMoto = km
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

        cargarEstadoAceite(motoId, km)
        cargarActividadReciente(motoId)
    }

    private fun iniciarRuta() {
        if (sharedVm.motoId == null) return
        RodandoEstado.kmInicio = kmActualMoto
        requireContext().startForegroundService(
            Intent(requireContext(), RodandoService::class.java)
        )
    }

    private fun finalizarRuta() {
        val distanciaM = RodandoEstado.distanciaMetros.value.toInt()
        val duracionS = RodandoEstado.tiempoSegundos.value.toInt()
        val kmInicio = RodandoEstado.kmInicio
        requireContext().stopService(Intent(requireContext(), RodandoService::class.java))

        viewLifecycleOwner.lifecycleScope.launch {
            val motoId = sharedVm.motoId ?: return@launch
            try {
                val kmFin = kmInicio + (distanciaM / 1000)
                SupabaseClient.client.postgrest.from("registros_ruta").insert(
                    RutaInsert(
                        moto_id = motoId,
                        km_inicio = kmInicio,
                        km_fin = kmFin,
                        distancia_m = distanciaM,
                        duracion_s = duracionS
                    )
                )
            } catch (_: Exception) { }
            cargarDashboard()
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun formatTiempo(segundos: Long): String {
        val h = segundos / 3600
        val m = (segundos % 3600) / 60
        val s = segundos % 60
        return "%02d:%02d:%02d".format(h, m, s)
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
                lleno.rvActividadReciente.adapter = HistorialAdapter(items.toMutableList()) { _, _ -> }
                lleno.rvActividadReciente.visibility = View.VISIBLE
                lleno.txtSinActividadInicio.visibility = View.GONE
            }
        } catch (_: Exception) { }
    }

    private suspend fun cargarEstadoAceite(motoId: String, kmActual: Int) {
        try {
            val aceite = SupabaseClient.client.postgrest.from("registros_mantenimiento")
                .select {
                    filter {
                        eq("moto_id", motoId)
                        ilike("tipo", "%aceite%")
                    }
                    order("kilometraje", Order.DESCENDING)
                    limit(1L)
                }
                .decodeList<AceiteRecord>().firstOrNull()

            if (_binding == null) return
            val lleno = binding.llenoContent

            if (aceite == null) {
                lleno.txtAceiteEstado.text = getString(R.string.label_sin_registro)
                lleno.txtAceiteEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
                lleno.progressAceite.progress = 0
                return
            }

            val intervalo = aceite.repetir_cada_km
            if (intervalo == null || intervalo <= 0) {
                val kmDesde = (kmActual - aceite.kilometraje).coerceAtLeast(0)
                lleno.txtAceiteEstado.text = getString(R.string.label_aceite_km_desde, kmDesde)
                lleno.txtAceiteEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
                lleno.progressAceite.progress = 0
                return
            }

            val kmDesde = (kmActual - aceite.kilometraje).coerceAtLeast(0)
            val progreso = ((kmDesde.toFloat() / intervalo) * 100).toInt().coerceIn(0, 100)
            val kmRestantes = intervalo - kmDesde
            lleno.progressAceite.progress = progreso

            when {
                kmRestantes <= 0 -> {
                    lleno.txtAceiteEstado.text = getString(R.string.label_aceite_pendiente)
                    lleno.txtAceiteEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_red))
                    lleno.progressAceite.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.alert_red)
                }
                kmRestantes <= 500 -> {
                    lleno.txtAceiteEstado.text = getString(R.string.label_aceite_km_restantes, kmRestantes)
                    lleno.txtAceiteEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow))
                    lleno.progressAceite.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.warning_yellow)
                }
                else -> {
                    lleno.txtAceiteEstado.text = getString(R.string.label_aceite_km_restantes, kmRestantes)
                    lleno.txtAceiteEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.cyan_primary))
                    lleno.progressAceite.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.cyan_primary)
                }
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
