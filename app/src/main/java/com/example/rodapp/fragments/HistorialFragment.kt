package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentHistorialBinding
import com.example.rodapp.models.HistorialItem
import com.example.rodapp.models.Moto
import com.example.rodapp.models.MotoResumen
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private var allMotos: List<Moto> = emptyList()
    private var allItemsGlobal: MutableList<HistorialItem> = mutableListOf()
    private var allItems: MutableList<HistorialItem> = mutableListOf()
    private var selectedMotoId: String? = null
    private var currentAdapter: HistorialAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistorial.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically() = false
        }
        binding.rvMotosResumen.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        configurarSwipeDelete()
        setupChipsFiltro()
        cargarMotosYHistorial()
    }

    private fun configurarSwipeDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = currentAdapter ?: return
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.removeAt(position)

                val indexAll = allItems.indexOfFirst { it.id == item.id }
                if (indexAll >= 0) allItems.removeAt(indexAll)
                val indexGlobal = allItemsGlobal.indexOfFirst { it.id == item.id }
                if (indexGlobal >= 0) allItemsGlobal.removeAt(indexGlobal)

                Snackbar.make(binding.root, getString(R.string.label_registro_eliminado), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.label_deshacer)) {
                        adapter.restoreAt(item, position)
                        if (indexAll >= 0) allItems.add(indexAll, item)
                        if (indexGlobal >= 0) allItemsGlobal.add(indexGlobal, item)
                        actualizarStats()
                        actualizarGrafica()
                        actualizarResumenMotos()
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(snackbar: Snackbar, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) ejecutarDelete(item)
                        }
                    })
                    .show()
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvHistorial)
    }

    private fun ejecutarDelete(item: HistorialItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tabla = if (item.tipo.lowercase() == "combustible")
                    "registros_combustible" else "registros_mantenimiento"
                SupabaseClient.client.postgrest.from(tabla)
                    .delete { filter { eq("id", item.id) } }
            } catch (_: Exception) { }
        }
    }

    private fun setupChipsFiltro() {
        binding.chipTodo.setOnCheckedChangeListener { _, checked ->
            if (checked) aplicarFiltro("todo")
        }
        binding.chipCombustible.setOnCheckedChangeListener { _, checked ->
            if (checked) aplicarFiltro("combustible")
        }
        binding.chipMantenimiento.setOnCheckedChangeListener { _, checked ->
            if (checked) aplicarFiltro("mantenimiento")
        }
    }

    private fun aplicarFiltro(filtro: String) {
        val filtrados = when (filtro) {
            "combustible" -> allItems.filter { it.tipo == "combustible" }
            "mantenimiento" -> allItems.filter { it.tipo == "mantenimiento" }
            else -> allItems.toList()
        }
        actualizarLista(filtrados)
    }

    private fun cargarMotosYHistorial() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                allMotos = SupabaseClient.client.postgrest.from("motos")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<Moto>()

                if (_binding == null) return@launch

                if (allMotos.isEmpty()) {
                    binding.rvHistorial.visibility = View.GONE
                    binding.layoutEmptyHistorial.visibility = View.VISIBLE
                    return@launch
                }

                allItemsGlobal = mutableListOf()
                for (moto in allMotos) {
                    val motoId = moto.id ?: continue
                    val items = SupabaseClient.client.postgrest.from("v_historial")
                        .select {
                            filter { eq("moto_id", motoId) }
                            order("created_at", Order.DESCENDING)
                            limit(100L)
                        }
                        .decodeList<HistorialItem>()
                    allItemsGlobal.addAll(items)
                }
                allItemsGlobal.sortByDescending { it.created_at }

                if (_binding == null) return@launch

                if (allMotos.size == 1) {
                    selectedMotoId = allMotos[0].id
                    binding.scrollMotoSelector.visibility = View.GONE
                } else {
                    selectedMotoId = null
                    setupMotoSelector(allMotos)
                    binding.scrollMotoSelector.visibility = View.VISIBLE
                }

                actualizarVistaMoto(selectedMotoId)

            } catch (_: Exception) {
                if (_binding != null) {
                    binding.rvHistorial.visibility = View.GONE
                    binding.layoutEmptyHistorial.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupMotoSelector(motos: List<Moto>) {
        binding.chipGroupMotos.removeAllViews()

        val chipTodas = Chip(requireContext()).apply {
            text = getString(R.string.label_todas_motos)
            isCheckable = true
            isChecked = (selectedMotoId == null)
        }
        chipTodas.setOnCheckedChangeListener { _, checked ->
            if (checked) actualizarVistaMoto(null)
        }
        binding.chipGroupMotos.addView(chipTodas)

        for (moto in motos) {
            val motoId = moto.id ?: continue
            val rawLabel = "${moto.marca} ${moto.modelo}"
            val label = if (rawLabel.length > 14) rawLabel.take(13) + "…" else rawLabel
            val chip = Chip(requireContext()).apply {
                text = label
                tag = motoId
                isCheckable = true
                isChecked = (selectedMotoId == motoId)
            }
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) actualizarVistaMoto(motoId)
            }
            binding.chipGroupMotos.addView(chip)
        }
    }

    private fun actualizarVistaMoto(motoId: String?) {
        selectedMotoId = motoId
        allItems = if (motoId == null) {
            allItemsGlobal.toMutableList()
        } else {
            allItemsGlobal.filter { it.moto_id == motoId }.toMutableList()
        }

        val mostrarResumen = motoId == null && allMotos.size > 1
        binding.lblResumenMotos.visibility = if (mostrarResumen) View.VISIBLE else View.GONE
        binding.rvMotosResumen.visibility = if (mostrarResumen) View.VISIBLE else View.GONE
        if (mostrarResumen) actualizarResumenMotos()

        actualizarStats()
        actualizarGrafica()

        val filtroActual = when {
            binding.chipCombustible.isChecked -> "combustible"
            binding.chipMantenimiento.isChecked -> "mantenimiento"
            else -> "todo"
        }
        aplicarFiltro(filtroActual)
    }

    private fun actualizarResumenMotos() {
        val resumenes = allMotos.mapNotNull { moto ->
            val id = moto.id ?: return@mapNotNull null
            val items = allItemsGlobal.filter { it.moto_id == id }
            val combustibles = items.filter { it.tipo == "combustible" }
            val mantenimientos = items.filter { it.tipo == "mantenimiento" }
            val gasto = combustibles.mapNotNull { it.valor?.toDoubleOrNull() }.sum()
            val eficiencia = calcularEficiencia(combustibles)
            MotoResumen(moto, gasto, eficiencia, mantenimientos.size)
        }
        binding.rvMotosResumen.adapter = MotoResumenAdapter(resumenes)
    }

    private fun actualizarStats() {
        val combustibles = allItems.filter { it.tipo == "combustible" }
        val mantenimientos = allItems.filter { it.tipo == "mantenimiento" }

        binding.txtCountMantenimiento.text = mantenimientos.size.toString()

        val totalComb = combustibles.mapNotNull { it.valor?.toDoubleOrNull() }.sum()
        binding.txtGastoTotal.text = if (totalComb > 0) "$${"%.0f".format(totalComb)}" else "—"

        val eficiencia = calcularEficiencia(combustibles)
        binding.txtEficiencia.text = if (eficiencia > 0) "${"%.1f".format(eficiencia)}" else "—"
    }

    private fun calcularEficiencia(combustibles: List<HistorialItem>): Double {
        val sorted = combustibles
            .filter { it.litros != null && it.litros > 0 && it.kilometraje != null }
            .sortedBy { it.created_at }

        if (sorted.size < 2) return 0.0

        var totalKm = 0
        var totalLitros = 0.0
        for (i in 1 until sorted.size) {
            val kmDiff = sorted[i].kilometraje!! - sorted[i - 1].kilometraje!!
            if (kmDiff > 0) {
                totalKm += kmDiff
                totalLitros += sorted[i].litros!!
            }
        }
        return if (totalLitros > 0) totalKm / totalLitros else 0.0
    }

    private fun actualizarGrafica() {
        val combustibles = allItems.filter { it.tipo == "combustible" }
        val today = LocalDate.now()

        val entries = (5 downTo 0).map { offset ->
            val mes = today.minusMonths(offset.toLong())
            val label = mes.month
                .getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-CO"))
                .replaceFirstChar { it.uppercase() }
            val total = combustibles
                .filter { item ->
                    val d = runCatching { LocalDate.parse(item.created_at.take(10)) }.getOrNull()
                    d?.year == mes.year && d.month == mes.month
                }
                .mapNotNull { it.valor?.toFloatOrNull() }
                .sum()
            HistorialChartView.BarEntry(label, total)
        }

        binding.chartGastos.setData(entries)
    }

    private fun actualizarLista(items: List<HistorialItem>) {
        if (items.isEmpty()) {
            binding.rvHistorial.visibility = View.GONE
            binding.layoutEmptyHistorial.visibility = View.VISIBLE
            currentAdapter = null
        } else {
            val adapter = HistorialAdapter(items.toMutableList()) { _, _ -> }
            currentAdapter = adapter
            binding.rvHistorial.adapter = adapter
            binding.rvHistorial.visibility = View.VISIBLE
            binding.layoutEmptyHistorial.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
