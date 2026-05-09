package com.example.rodapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentHistorialBinding
import com.example.rodapp.models.HistorialItem
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

    private var allItems: List<HistorialItem> = emptyList()

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
        setupChips()
        cargarHistorial()
    }

    private fun setupChips() {
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
            else -> allItems
        }
        actualizarLista(filtrados)
    }

    private fun cargarHistorial() {
        val motoId = sharedVm.motoId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                allItems = SupabaseClient.client.postgrest.from("v_historial")
                    .select {
                        filter { eq("moto_id", motoId) }
                        order("created_at", Order.DESCENDING)
                        limit(100L)
                    }
                    .decodeList<HistorialItem>()

                if (_binding == null) return@launch

                actualizarStats()
                actualizarGrafica()
                aplicarFiltro("todo")

            } catch (_: Exception) {
                if (_binding != null) {
                    binding.rvHistorial.visibility = View.GONE
                    binding.layoutEmptyHistorial.visibility = View.VISIBLE
                }
            }
        }
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
        } else {
            binding.rvHistorial.adapter = HistorialAdapter(items)
            binding.rvHistorial.visibility = View.VISIBLE
            binding.layoutEmptyHistorial.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
