package com.example.rodapp.activities.inicio

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.activities.main.MainActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class firstActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        lifecycleScope.launch {
            delay(2500)
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser != null) {
                startActivity(Intent(this@firstActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@firstActivity, Start_activity::class.java))
            }
            finish()
        }
    }
}
