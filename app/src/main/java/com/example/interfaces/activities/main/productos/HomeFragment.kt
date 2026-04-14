package com.example.interfaces.activities.main.productos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.interfaces.R

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment() {
    private val listaProduct = listOf(
        Product(nombre = "motor 150cc", precio = 495.000, imagenRes = R.drawable.motor150),
        Product(nombre = "motor 200cc", precio = 680.000, imagenRes = R.drawable.motor200),
        Product(nombre = "motor 250cc", precio = 680.000, imagenRes = R.drawable.motor250),
        Product(nombre = "motor 400cc", precio = 680.000, imagenRes = R.drawable.motor400),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_productos)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = ProductAdapter(listaProduct)
        return view
    }
}
