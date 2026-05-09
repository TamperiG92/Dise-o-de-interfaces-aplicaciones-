package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.databinding.FragmentDocumentosAdicionalesBinding

class DocumentosAdicionalesFragment : Fragment() {

    private var _binding: FragmentDocumentosAdicionalesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentosAdicionalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackDocsAdic.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddLicencia.setOnClickListener {
            findNavController().navigate(
                R.id.navigation_nuevo_documento,
                Bundle().apply { putString("tipo_documento", "LICENCIA") }
            )
        }

        binding.btnAddTodoRiesgo.setOnClickListener {
            findNavController().navigate(
                R.id.navigation_nuevo_documento,
                Bundle().apply { putString("tipo_documento", "SEGURO_TODO_RIESGO") }
            )
        }

        binding.btnCrearDocPersonalizado.setOnClickListener {
            findNavController().navigate(
                R.id.navigation_nuevo_documento,
                Bundle().apply { putString("tipo_documento", "PERSONALIZADO") }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
