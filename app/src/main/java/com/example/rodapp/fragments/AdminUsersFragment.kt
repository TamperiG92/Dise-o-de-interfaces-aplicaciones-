package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rodapp.R

class AdminUsersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_users, container, false)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_users)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = MockAdminUserAdapter()
        
        return view
    }
}

class MockAdminUserAdapter : RecyclerView.Adapter<MockAdminUserAdapter.ViewHolder>() {

    // Datos estáticos copiados desde tu diseño en Figma
    private val mockData = listOf(
        Triple("Julian Diaz", "MT-09 SP", Pair(true, true)),
        Triple("Sara Rios", "Duke 390", Pair(true, false)),
        Triple("Alex Mora", "R6 Raven", Pair(true, true)),
        Triple("M. Vera", "V-Strom 650", Pair(false, true)),
        Triple("Luis Paez", "S1000RR", Pair(true, true))
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tv_avatar_initials)
        val tvName: TextView = view.findViewById(R.id.tv_user_name)
        val tvMoto: TextView = view.findViewById(R.id.tv_motorcycle)
        val dot1: View = view.findViewById(R.id.dot_1)
        val dot2: View = view.findViewById(R.id.dot_2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, moto, docs) = mockData[position]
        
        // Calcular iniciales (hasta dos letras)
        holder.tvInitials.text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
        
        holder.tvName.text = name
        holder.tvMoto.text = moto
        
        // Logica de puntitos de color
        holder.dot1.backgroundTintList = android.content.res.ColorStateList.valueOf(
            holder.itemView.context.resources.getColor(if(docs.first) R.color.cyan_primary else R.color.alert_red, null)
        )
        holder.dot2.backgroundTintList = android.content.res.ColorStateList.valueOf(
            holder.itemView.context.resources.getColor(if(docs.second) R.color.cyan_primary else R.color.alert_red, null)
        )
    }

    override fun getItemCount() = mockData.size
}
