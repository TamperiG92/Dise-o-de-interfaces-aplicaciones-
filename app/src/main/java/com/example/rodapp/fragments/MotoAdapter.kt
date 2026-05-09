package com.example.rodapp.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rodapp.databinding.ItemMotoBinding
import com.example.rodapp.models.Moto

class MotoAdapter(
    private val motos: List<Moto>,
    private val onMotoClick: (Moto) -> Unit
) : RecyclerView.Adapter<MotoAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMotoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val moto = motos[position]
        holder.binding.txtMotoNombre.text = "${moto.marca} ${moto.modelo}"
        holder.binding.txtMotoPlaca.text = moto.placa
        holder.binding.root.setOnClickListener { onMotoClick(moto) }
    }

    override fun getItemCount() = motos.size
}
