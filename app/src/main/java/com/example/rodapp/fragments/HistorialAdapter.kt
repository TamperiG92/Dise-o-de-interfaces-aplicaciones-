package com.example.rodapp.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rodapp.R
import com.example.rodapp.databinding.ItemHistorialBinding
import com.example.rodapp.models.HistorialItem

class HistorialAdapter(
    private val items: MutableList<HistorialItem>,
    private val onDelete: (HistorialItem, Int) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHistorialBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistorialBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.binding.root.context

        val (iconRes, iconTint, label) = when (item.tipo.lowercase()) {
            "combustible" -> Triple(
                android.R.drawable.ic_menu_add,
                R.color.button_blue,
                item.subtipo ?: ctx.getString(R.string.label_combustible)
            )
            "mantenimiento" -> Triple(
                android.R.drawable.ic_menu_manage,
                R.color.orange_secondary,
                item.subtipo ?: ctx.getString(R.string.label_mantenimiento)
            )
            "ruta" -> Triple(
                android.R.drawable.ic_menu_directions,
                R.color.success_green,
                item.subtipo ?: ctx.getString(R.string.label_ruta)
            )
            else -> Triple(
                android.R.drawable.ic_menu_agenda,
                R.color.button_blue,
                item.subtipo ?: ctx.getString(R.string.label_documento)
            )
        }

        holder.binding.imgTipoHistorial.setImageResource(iconRes)
        holder.binding.imgTipoHistorial.setColorFilter(ctx.getColor(iconTint))
        holder.binding.txtSubtipoHistorial.text = label

        val fecha = item.created_at.take(10)
        val km = item.kilometraje?.let { " · $it km" } ?: ""
        holder.binding.txtFechaHistorial.text = "$fecha$km"

        holder.binding.txtValorHistorial.text = item.valor?.let {
            if (item.tipo == "combustible") "$$it" else it
        } ?: ""
    }

    fun removeAt(position: Int): HistorialItem {
        val item = items.removeAt(position)
        notifyItemRemoved(position)
        return item
    }

    fun restoreAt(item: HistorialItem, position: Int) {
        items.add(position, item)
        notifyItemInserted(position)
    }
}
