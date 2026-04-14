package com.example.interfaces.activities.inicio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.interfaces.R
import com.example.interfaces.activities.auth.login
import com.example.interfaces.activities.auth.register_user

class Start_activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnComienza = findViewById<Button>(R.id.boton_comienza)
        val btnRegistrar = findViewById<TextView>(R.id.texto_registrate_start)

        btnComienza.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
        }

        btnRegistrar.setOnClickListener {
            val intent = Intent(this, register_user::class.java)
            startActivity(intent)
        }
    }
}
