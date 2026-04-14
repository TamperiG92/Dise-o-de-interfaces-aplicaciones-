package com.example.interfaces.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.interfaces.R
import com.example.interfaces.activities.SupabaseClient
import com.example.interfaces.activities.main.MainActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etUsuario = findViewById<EditText>(R.id.usuario_login)
        val etContrasena = findViewById<EditText>(R.id.contrasena_login)
        val botonIngresar = findViewById<Button>(R.id.boton_ingresar)
        val botonRegistrate = findViewById<TextView>(R.id.boton_registrate)

        botonIngresar.setOnClickListener {
            val correo = etUsuario.text.toString().trim()
            val pass = etContrasena.text.toString().trim()

            if (correo.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Aquí implementamos la consulta real a Supabase
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        email = correo
                        password = pass
                    }
                    
                    // Si el login es exitoso
                    runOnUiThread {
                        Toast.makeText(this@login, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@login, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        val errorMsg = when {
                            e.message?.contains("Invalid login credentials") == true -> "Correo o contraseña incorrectos"
                            else -> "Error: ${e.message}"
                        }
                        Toast.makeText(this@login, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        botonRegistrate.setOnClickListener {
            val intent = Intent(this, register_user::class.java)
            startActivity(intent)
        }
    }
}
