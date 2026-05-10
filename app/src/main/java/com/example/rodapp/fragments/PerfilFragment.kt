package com.example.rodapp.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import com.example.rodapp.models.Moto
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
import java.io.ByteArrayOutputStream

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

        // Mostrar cache local inmediatamente mientras carga desde Supabase
        cargarDesdeCache()

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

    private fun cargarDesdeCache() {
        val prefs = requireContext().getSharedPreferences("perfil_cache", Context.MODE_PRIVATE)
        val nombre = prefs.getString("nombre", null)
        val correo = prefs.getString("correo", null)
        if (!nombre.isNullOrEmpty()) binding.txtNombrePerfil.text = nombre
        if (!correo.isNullOrEmpty()) binding.txtCorreoValor.text = correo
    }

    private fun guardarEnCache(nombre: String, correo: String) {
        requireContext().getSharedPreferences("perfil_cache", Context.MODE_PRIVATE)
            .edit()
            .putString("nombre", nombre)
            .putString("correo", correo)
            .apply()
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
            guardarEnCache(usuario.name, usuario.correo ?: "")

            prefs?.let {
                binding.switchPush.isChecked = it.notificaciones_push
                binding.switchAlerts.isChecked = it.alertas_mantenimiento
            }
            prefsLoaded = true

            // Cargar foto de perfil
            usuario.url_photo?.takeIf { it.isNotEmpty() }?.let { url ->
                cargarFotoDesdeUrl(url)
            }

            // Cargar foto de la moto en el header
            val motoId = sharedVm.motoId
            if (motoId != null) {
                val moto = SupabaseClient.client.postgrest.from("motos")
                    .select { filter { eq("id", motoId) }; limit(1L) }
                    .decodeList<Moto>().firstOrNull()
                moto?.foto_url?.takeIf { it.isNotEmpty() }?.let { fotoMoto ->
                    if (_binding != null && usuario.url_photo.isNullOrEmpty()) {
                        cargarFotoDesdeUrl(fotoMoto)
                    }
                }
            }
        } catch (_: Exception) {
            if (_binding != null) prefsLoaded = true
        }
    }

    private suspend fun cargarFotoDesdeUrl(url: String) {
        try {
            val bitmap = withContext(Dispatchers.IO) {
                java.net.URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        val raw = inputStream.readBytes()
                        corregirRotacion(uri, raw)
                    }
                } ?: return@launch

                // Mostrar preview local
                if (_binding != null) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.imgPerfil.setImageBitmap(bmp)
                    binding.imgPerfil.imageTintList = null
                    binding.imgPerfil.setPadding(0, 0, 0, 0)
                }

                val path = "$userId/perfil.jpg"
                SupabaseClient.client.storage.from("avatars").upload(path, bytes) { upsert = true }
                val url = SupabaseClient.client.storage.from("avatars").publicUrl(path)
                SupabaseClient.client.postgrest.from("users")
                    .update(UserPhotoUpdate(url)) { filter { eq("id", userId) } }
                guardarEnCache(binding.txtNombrePerfil.text.toString(), binding.txtCorreoValor.text.toString())
                toast(getString(R.string.foto_actualizada))
            } catch (_: Exception) {
                toast(getString(R.string.error_inesperado))
            } finally {
                if (_binding != null) binding.fabCameraPerfil.isEnabled = true
            }
        }
    }

    private fun corregirRotacion(uri: Uri, rawBytes: ByteArray): ByteArray {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
                val rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (rotation == 0f) return rawBytes
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes
                val matrix = Matrix().apply { postRotate(rotation) }
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                val out = ByteArrayOutputStream()
                rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.toByteArray()
            } ?: rawBytes
        } catch (_: Exception) { rawBytes }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
