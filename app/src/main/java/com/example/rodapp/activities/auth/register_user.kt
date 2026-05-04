package com.example.rodapp.activities.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.databinding.ActivityRegisterUserBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(val id: String, val name: String, val lastname: String)

class register_user : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCrearCuenta.setOnClickListener { performRegister() }

        binding.btnIrLogin.setOnClickListener {
            startActivity(
                Intent(this, login::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            finish()
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun performRegister() {
        val nombre = binding.etNombre.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val pass = binding.etPass.text.toString()

        if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()) {
            toast(getString(R.string.error_campos_vacios))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            toast(getString(R.string.error_email_invalido))
            return
        }
        if (pass.length < 8) {
            toast(getString(R.string.error_pass_corta))
            return
        }
        if (!binding.checkTerminos.isChecked) {
            toast(getString(R.string.error_terminos))
            return
        }

        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    email = correo
                    password = pass
                }
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    SupabaseClient.client.postgrest.from("users")
                        .insert(UserProfile(id = userId, name = nombre, lastname = ""))
                }
                toast(getString(R.string.cuenta_creada))
                startActivity(
                    Intent(this@register_user, login::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                finish()
            } catch (e: Exception) {
                toast(mapRegisterError(e))
            }
        }
    }

    private fun mapRegisterError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("User already registered", ignoreCase = true) ->
                getString(R.string.error_usuario_existente)
            msg.contains("network", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                getString(R.string.error_conexion)
            else -> getString(R.string.error_inesperado)
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
