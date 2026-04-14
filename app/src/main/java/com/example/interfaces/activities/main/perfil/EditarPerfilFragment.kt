package com.example.interfaces.activities.main.perfil

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.example.interfaces.R

class EditarPerfilFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_editar_perfil, container, false)

        val btnGuardar = view.findViewById<Button>(R.id.btn_guardar_perfil)
        btnGuardar.setOnClickListener {
            Toast.makeText(context, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
