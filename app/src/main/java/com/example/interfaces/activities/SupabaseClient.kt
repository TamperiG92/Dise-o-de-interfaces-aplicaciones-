package com.example.interfaces.activities

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://newjkmfwtsonggcsahvg.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ld2prbWZ3dHNvbmdnY3NhaHZnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU1MTY1NjksImV4cCI6MjA5MTA5MjU2OX0.1heUe_20noJw6Dq4T08DsEsnEYF5ks_25uEpfey95tQ"
    ) {
        install(Auth)
        install(Postgrest)
    }
}
