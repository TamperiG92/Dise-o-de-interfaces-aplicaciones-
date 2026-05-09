package com.example.rodapp.activities.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.rodapp.SupabaseClient
import com.example.rodapp.activities.main.AdminActivity
import com.example.rodapp.activities.main.MainActivity
import com.example.rodapp.databinding.ActivityLoginBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
private data class UserRole(val role: String)

class login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleAuthRedirect(intent)
        observeSessionStatus()
        setupListeners()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthRedirect(intent)
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { performEmailLogin() }
        binding.btnGoogle.setOnClickListener { performGoogleLogin() }
        binding.btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, register_user::class.java))
        }
    }

    private fun performEmailLogin() {
        val correo = binding.etCorreo.text.toString().trim()
        val pass = binding.etPass.text.toString()

        if (correo.isEmpty() || pass.isEmpty()) {
            toast(getString(com.example.rodapp.R.string.error_campos_vacios))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            toast(getString(com.example.rodapp.R.string.error_email_invalido))
            return
        }

        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    email = correo
                    password = pass
                }
            } catch (e: Exception) {
                toast(mapAuthError(e))
            }
        }
    }

    private fun performGoogleLogin() {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Google, redirectUrl = "rodapp://login") {
                    queryParams["prompt"] = "select_account"
                }
            } catch (e: Exception) {
                toast(getString(com.example.rodapp.R.string.error_inesperado))
            }
        }
    }

    private fun observeSessionStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SupabaseClient.client.auth.sessionStatus.collect { status ->
                    if (!isNavigating && status is SessionStatus.Authenticated && status.isNew) {
                        isNavigating = true
                        navigateToMain()
                    }
                }
            }
        }
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val data = intent?.dataString ?: return
        if (data.contains("#access_token=") || data.contains("error_description=")) {
            try {
                SupabaseClient.client.handleDeeplinks(intent)
            } catch (e: Exception) {
                toast(getString(com.example.rodapp.R.string.error_inesperado))
            }
        }
    }

    private fun navigateToMain() {
        lifecycleScope.launch {
            val targetClass = try {
                val authUser = SupabaseClient.client.auth.currentUserOrNull()
                val uid = authUser?.id ?: ""

                val existing = SupabaseClient.client.postgrest.from("users")
                    .select {
                        filter { eq("id", uid) }
                        limit(1L)
                    }
                    .decodeList<UserRole>()

                if (existing.isEmpty()) {
                    // Primer login OAuth (Google, etc.) — crear fila en users
                    val email = authUser?.email ?: ""
                    val meta = authUser?.userMetadata
                    val name = (meta?.get("full_name") as? JsonPrimitive)?.content
                        ?: (meta?.get("name") as? JsonPrimitive)?.content
                        ?: email.substringBefore("@")
                    SupabaseClient.client.postgrest.from("users").insert(
                        UserProfile(id = uid, name = name, lastname = "", correo = email)
                    )
                    MainActivity::class.java
                } else {
                    if (existing.first().role == "admin") AdminActivity::class.java
                    else MainActivity::class.java
                }
            } catch (_: Exception) {
                MainActivity::class.java
            }
            val intent = Intent(this@login, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                getString(com.example.rodapp.R.string.error_credenciales)
            msg.contains("network", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                getString(com.example.rodapp.R.string.error_conexion)
            else -> getString(com.example.rodapp.R.string.error_inesperado)
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
