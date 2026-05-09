package com.example.rodapp.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.rodapp.activities.main.MainActivity
import com.example.rodapp.databinding.FragmentPerfilBinding
import com.example.rodapp.models.UserPreferences
import com.example.rodapp.models.UsuarioInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable private data class UserPhotoUpdate(val url_photo: String)
@Serializable private data class NotifPushUpdate(val notificaciones_push: Boolean)
@Serializable private data class AlertasMantUpdate(val alertas_mantenimiento: Boolean)

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val sharedVm: SharedViewModel by activityViewModels()

    private var prefsLoaded = false

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        subirFotoPerfil(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackPerfil.setOnClickListener { findNavController().navigateUp() }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_logout_title))
                .setMessage(getString(R.string.dialog_logout_message))
                .setPositiveButton(getString(R.string.btn_logout)) { _, _ ->
                    (requireActivity() as MainActivity).cerrarSesion()
                }
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show()
        }

        binding.fabCameraPerfil.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.rowPassword.setOnClickListener {
            findNavController().navigate(R.id.navigation_cambiar_password)
        }

        configurarSwitches()

        sharedVm.motoNombre?.let { binding.txtMotoPerfil.text = it }
        viewLifecycleOwner.lifecycleScope.launch { cargarUsuario() }
    }

    private fun configurarSwitches() {
        binding.switchPush.setOnCheckedChangeListener { _, isChecked ->
            if (!prefsLoaded) return@setOnCheckedChangeListener
            guardarPreferencia(isChecked, soloAlertas = false)
        }
        binding.switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            if (!prefsLoaded) return@setOnCheckedChangeListener
            guardarPreferencia(isChecked, soloAlertas = true)
        }
    }

    private fun guardarPreferencia(valor: Boolean, soloAlertas: Boolean) {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (soloAlertas) {
                    SupabaseClient.client.postgrest.from("user_preferences")
                        .update(AlertasMantUpdate(valor)) { filter { eq("user_id", userId) } }
                } else {
                    SupabaseClient.client.postgrest.from("user_preferences")
                        .update(NotifPushUpdate(valor)) { filter { eq("user_id", userId) } }
                }
            } catch (_: Exception) { }
        }
    }

    private suspend fun cargarUsuario() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        try {
            val usuario = SupabaseClient.client.postgrest.from("users")
                .select { filter { eq("id", userId) }; limit(1L) }
                .decodeList<UsuarioInfo>().firstOrNull() ?: return

            val prefs = SupabaseClient.client.postgrest.from("user_preferences")
                .select { filter { eq("user_id", userId) }; limit(1L) }
                .decodeList<UserPreferences>().firstOrNull()

            if (_binding == null) return

            binding.txtNombrePerfil.text = usuario.name
            binding.txtCorreoValor.text = usuario.correo ?: ""

            prefs?.let {
                binding.switchPush.isChecked = it.notificaciones_push
                binding.switchAlerts.isChecked = it.alertas_mantenimiento
            }
            prefsLoaded = true

            usuario.url_photo?.takeIf { it.isNotEmpty() }?.let { url ->
                cargarFotoDesdeUrl(url)
            }
        } catch (_: Exception) {
            if (_binding != null) prefsLoaded = true
        }
    }

    private suspend fun cargarFotoDesdeUrl(url: String) {
        try {
            val bitmap = withContext(Dispatchers.IO) {
                java.net.URL(url).openStream().use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            }
            if (_binding != null && bitmap != null) {
                binding.imgPerfil.setImageBitmap(bitmap)
                binding.imgPerfil.imageTintList = null
                binding.imgPerfil.setPadding(0, 0, 0, 0)
            }
        } catch (_: Exception) { }
    }

    private fun subirFotoPerfil(uri: Uri) {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        binding.fabCameraPerfil.isEnabled = false
        binding.imgPerfil.setImageURI(uri)
        binding.imgPerfil.imageTintList = null
        binding.imgPerfil.setPadding(0, 0, 0, 0)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bytes = requireContext().contentResolver
                    .openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val path = "$userId/perfil.jpg"
                SupabaseClient.client.storage.from("avatars").upload(path, bytes) { upsert = true }
                val url = SupabaseClient.client.storage.from("avatars").publicUrl(path)
                SupabaseClient.client.postgrest.from("users")
                    .update(UserPhotoUpdate(url)) { filter { eq("id", userId) } }
                toast(getString(R.string.foto_actualizada))
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
            } finally {
                if (_binding != null) binding.fabCameraPerfil.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
