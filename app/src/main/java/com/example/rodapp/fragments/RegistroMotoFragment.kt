package com.example.rodapp.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rodapp.R
import com.example.rodapp.SharedViewModel
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.FragmentRegistroMotoBinding
import com.example.rodapp.models.Moto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import android.util.Log
import kotlinx.coroutines.launch

class RegistroMotoFragment : Fragment() {

    private var _binding: FragmentRegistroMotoBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private var selectedPhotoUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        selectedPhotoUri = uri
        binding.imgMotoPreview.setImageURI(uri)
        binding.imgMotoPreview.alpha = 1f
        binding.imgMotoPreview.clearColorFilter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroMotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val marcas = resources.getStringArray(R.array.marcas_moto)
        binding.spinnerMarca.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, marcas
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.cardIconMoto.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnGuardarEmpezar.setOnClickListener { guardarMoto() }
    }

    private fun guardarMoto() {
        val posicion = binding.spinnerMarca.selectedItemPosition
        val marca = binding.spinnerMarca.selectedItem?.toString() ?: ""
        val modelo = binding.etModelo.text.toString().trim()
        val cilindradaStr = binding.etCilindrada.text.toString().trim()
        val placa = binding.etPlaca.text.toString().trim().uppercase()
        val odometroStr = binding.etOdometro.text.toString().trim()

        if (posicion == 0 || modelo.isEmpty() || placa.isEmpty() || odometroStr.isEmpty()) {
            toast(getString(R.string.error_campos_vacios))
            return
        }

        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            Log.e("RegistroMoto", "userId is null — session not active")
            toast(getString(R.string.error_inesperado))
            return
        }

        Log.d("RegistroMoto", "userId=$userId, placa=$placa, marca=$marca, modelo=$modelo")
        binding.btnGuardarEmpezar.isEnabled = false
        binding.progressGuardando.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var fotoUrl: String? = null
                selectedPhotoUri?.let { uri ->
                    try {
                        val bytes = requireContext().contentResolver
                            .openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null) {
                            val path = "$userId/motos/${System.currentTimeMillis()}.jpg"
                            SupabaseClient.client.storage.from("avatars").upload(path, bytes) { upsert = true }
                            fotoUrl = SupabaseClient.client.storage.from("avatars").publicUrl(path)
                        }
                    } catch (e: Exception) {
                        Log.w("RegistroMoto", "foto upload failed: ${e.message}")
                    }
                }

                SupabaseClient.client.postgrest.from("motos").insert(
                    Moto(
                        user_id = userId,
                        marca = marca,
                        modelo = modelo,
                        cilindrada = cilindradaStr.toIntOrNull(),
                        placa = placa,
                        odometro_inicial = odometroStr.toIntOrNull() ?: 0,
                        foto_url = fotoUrl
                    )
                )

                val motos = SupabaseClient.client.postgrest.from("motos")
                    .select {
                        filter { eq("placa", placa) }
                    }
                    .decodeList<Moto>()

                val moto = motos.firstOrNull()
                sharedVm.motoId = moto?.id
                sharedVm.motoNombre = "$marca $modelo"

                toast(getString(R.string.moto_registrada))
                findNavController().navigateUp()
            } catch (e: Exception) {
                Log.e("RegistroMoto", "insert error: ${e.javaClass.simpleName}: ${e.message}", e)
                val msg = e.message ?: ""
                val error = when {
                    msg.contains("unique", true) || msg.contains("duplicate", true) ->
                        getString(R.string.error_placa_duplicada)
                    else -> getString(R.string.error_inesperado)
                }
                toast(error)
                binding.btnGuardarEmpezar.isEnabled = true
                binding.progressGuardando.visibility = View.GONE
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
