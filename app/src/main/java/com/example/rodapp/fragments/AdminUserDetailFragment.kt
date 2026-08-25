package com.example.rodapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.databinding.FragmentAdminUserDetailBinding

class AdminUserDetailFragment : Fragment() {

    private var _binding: FragmentAdminUserDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackAdminDetail.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRejectSoat.setOnClickListener {
            mostrarDialogoRechazo()
        }

        binding.btnRejectAll.setOnClickListener {
            mostrarDialogoRechazo()
        }

        binding.btnApproveAll.setOnClickListener {
            // Lógica para aprobar documentos
            findNavController().navigateUp()
        }

        // Lógica para desactivar cuenta (botón status)
        binding.btnStatusUser.setOnClickListener {
            mostrarDialogoDesactivar()
        }
    }

    private fun mostrarDialogoRechazo() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_rechazo_admin, null)
        val etMotivo = dialogView.findViewById<EditText>(R.id.et_motivo_rechazo)
        
        builder.setView(dialogView)
        builder.setPositiveButton("Enviar") { _, _ ->
            val motivo = etMotivo.text.toString()
            // Aquí enviarías el motivo al historial y al usuario
            findNavController().navigateUp()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoDesactivar() {
        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Desactivar Cuenta")
            .setMessage("¿Estás seguro de que deseas desactivar esta cuenta? El usuario perderá acceso temporalmente.")
            .setPositiveButton("Confirmar") { _, _ ->
                // Lógica de desactivación
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
