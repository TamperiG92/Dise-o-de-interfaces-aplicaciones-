package com.example.rodapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class IdOnly(val id: String)

class AdminDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
        loadStats(view)
        return view
    }

    private fun loadStats(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            var anyLimited = false

            try {
                val count = SupabaseClient.client.postgrest.from("users")
                    .select().decodeList<IdOnly>().size
                setStatText(view, R.id.tv_stat_users, count)
                if (count <= 1) anyLimited = true
            } catch (_: Exception) {
                setStatText(view, R.id.tv_stat_users, "—")
                anyLimited = true
            }

            try {
                val count = SupabaseClient.client.postgrest.from("motos")
                    .select().decodeList<IdOnly>().size
                setStatText(view, R.id.tv_stat_motos, count)
            } catch (_: Exception) {
                setStatText(view, R.id.tv_stat_motos, "—")
                anyLimited = true
            }

            try {
                val count = SupabaseClient.client.postgrest.from("registros_combustible")
                    .select().decodeList<IdOnly>().size
                setStatText(view, R.id.tv_stat_fuel, count)
            } catch (_: Exception) {
                setStatText(view, R.id.tv_stat_fuel, "—")
                anyLimited = true
            }

            try {
                val count = SupabaseClient.client.postgrest.from("registros_mantenimiento")
                    .select().decodeList<IdOnly>().size
                setStatText(view, R.id.tv_stat_maint, count)
            } catch (_: Exception) {
                setStatText(view, R.id.tv_stat_maint, "—")
                anyLimited = true
            }

            if (anyLimited) {
                view.findViewById<View>(R.id.card_rls_warning).visibility = View.VISIBLE
            }
        }
    }

    private fun setStatText(view: View, id: Int, value: Any) {
        view.findViewById<TextView>(id).text = value.toString()
    }
}
