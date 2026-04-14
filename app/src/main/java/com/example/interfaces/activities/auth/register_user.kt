package com.example.interfaces.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.interfaces.R
import com.example.interfaces.activities.SupabaseClient
import com.example.interfaces.models.UsuarioData
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class register_user : AppCompatActivity() {
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etReContrasena: EditText
    private lateinit var checkTerminos: CheckBox
    private lateinit var btnRegistro: Button
    private lateinit var tvCuenta: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_user)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etNombres = findViewById(R.id.nombre_registro)
        etApellidos = findViewById(R.id.apellido_registro)
        etCorreo = findViewById(R.id.correo_registro)
        etContrasena = findViewById(R.id.contrasena_registro)
        etReContrasena = findViewById(R.id.confirmar_contrasena_registro)
        checkTerminos = findViewById(R.id.check_terminos)
        btnRegistro = findViewById(R.id.boton_crear_cuenta)
        tvCuenta = findViewById(R.id.re_cuenta)
    }

    private fun setupListeners() {
        btnRegistro.setOnClickListener {
            val nombres = etNombres.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            val reContrasena = etReContrasena.text.toString().trim()

            // Validaciones
            if (nombres.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || reContrasena.isEmpty()) {
                Toast.makeText(this, "Por favor completa toda la información", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkTerminos.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena != reContrasena) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena.length < 8) {
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Paso 1: Auth
                    SupabaseClient.client.auth.signUpWith(Email) {
                        email = correo
                        password = contrasena
                    }

                    // Paso 2: Postgrest (Tabla users: id, name, lastname)
                    val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
                    SupabaseClient.client.postgrest["users"].insert(
                        UsuarioData(id = userId, name = nombres, lastname = apellidos)
                    )

                    runOnUiThread {
                        Toast.makeText(this@register_user, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@register_user, login::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    val mensajeError = if (e.message?.contains("already_exists") == true) {
                        "Este correo ya está registrado."
                    } else {
                        "Error: ${e.message}"
                    }
                    Toast.makeText(this@register_user, mensajeError, Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvCuenta.setOnClickListener {
            startActivity(Intent(this, login::class.java))
            finish()
        }
    }
}
