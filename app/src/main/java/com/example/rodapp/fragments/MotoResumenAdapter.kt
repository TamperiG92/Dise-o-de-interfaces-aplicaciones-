package com.example.rodapp.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rodapp.databinding.ItemMotoResumenBinding
import com.example.rodapp.models.MotoResumen

class MotoResumenAdapter(
    private val items: List<MotoResumen>
) : RecyclerView.Adapter<MotoResumenAdapter.VH>() {

    inner class VH(val binding: ItemMotoResumenBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMotoResumenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val resumen = items[position]
        val moto = resumen.moto
        with(holder.binding) {
            txtMotoNombre.text = "${moto.marca} ${moto.modelo}"
            txtMotoPlaca.text = moto.placa
            txtMotoGasto.text = if (resumen.gastoTotal > 0) "$${"%.0f".format(resumen.gastoTotal)}" else "—"
            txtMotoEficiencia.text = if (resumen.eficiencia > 0) "${"%.1f".format(resumen.eficiencia)} km/L" else "—"
        }
    }
}
