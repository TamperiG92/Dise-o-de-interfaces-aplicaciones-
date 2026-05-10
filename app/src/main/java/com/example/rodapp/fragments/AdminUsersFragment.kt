package com.example.rodapp.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.models.AdminUserInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class RoleUpdate(val role: String)

private enum class RoleFilter { ALL, ADMINS, CLIENTS }

class AdminUsersFragment : Fragment() {

    private var allUsers = listOf<AdminUserInfo>()
    private lateinit var adapter: AdminUserAdapter
    private lateinit var tvTotalCount: TextView
    private lateinit var tvAdminCount: TextView
    private var currentFilter = RoleFilter.ALL
    private var currentQuery = ""
    private var currentUserId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_users, container, false)

        currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

        tvTotalCount = view.findViewById(R.id.tv_total_users_count)
        tvAdminCount = view.findViewById(R.id.tv_admin_count)

        adapter = AdminUserAdapter(
            users = emptyList(),
            currentUserId = currentUserId,
            onRoleChange = { user, newRole -> promptRoleChange(user, newRole) },
            onDeleteClick = { user -> confirmDeleteUser(user) }
        )

        view.findViewById<RecyclerView>(R.id.recycler_users).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AdminUsersFragment.adapter
        }

        view.findViewById<TextInputLayout>(R.id.search_layout)?.editText
            ?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    currentQuery = s?.toString() ?: ""
                    applyFilters()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

        view.findViewById<MaterialButton>(R.id.btn_filter)?.setOnClickListener {
            showFilterDialog()
        }

        loadUsers()
        return view
    }

    private fun applyFilters() {
        var filtered = allUsers
        filtered = when (currentFilter) {
            RoleFilter.ADMINS -> filtered.filter { it.role == "admin" }
            RoleFilter.CLIENTS -> filtered.filter { it.role != "admin" }
            RoleFilter.ALL -> filtered
        }
        if (currentQuery.isNotBlank()) {
            filtered = filtered.filter { u ->
                u.name.contains(currentQuery, ignoreCase = true) ||
                (u.lastname?.contains(currentQuery, ignoreCase = true) == true) ||
                (u.correo?.contains(currentQuery, ignoreCase = true) == true)
            }
        }
        adapter.updateList(filtered)
    }

    private fun showFilterDialog() {
        val options = arrayOf(
            getString(R.string.filtro_todos),
            getString(R.string.filtro_solo_admins),
            getString(R.string.filtro_solo_clientes)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.title_filtrar_usuarios))
            .setSingleChoiceItems(options, currentFilter.ordinal) { dialog, which ->
                currentFilter = RoleFilter.values()[which]
                applyFilters()
                dialog.dismiss()
            }
            .show()
    }

    private fun loadUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val users = SupabaseClient.client.postgrest.from("users")
                    .select()
                    .decodeList<AdminUserInfo>()
                allUsers = users
                applyFilters()
                updateCounters()
                if (users.isEmpty()) showNoUsersToast()
            } catch (_: Exception) {
                showNoUsersToast()
                tvTotalCount.text = "—"
                tvAdminCount.text = "—"
            }
        }
    }

    private fun updateCounters() {
        tvTotalCount.text = allUsers.size.toString()
        tvAdminCount.text = allUsers.count { it.role == "admin" }.toString()
    }

    private fun showNoUsersToast() {
        Toast.makeText(requireContext(), getString(R.string.label_sin_usuarios), Toast.LENGTH_LONG).show()
    }

    private fun promptRoleChange(user: AdminUserInfo, newRole: String) {
        val displayName = "${user.name} ${user.lastname ?: ""}".trim()
        val msgRes = if (newRole == "admin") R.string.dialog_promover_msg else R.string.dialog_degradar_msg
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_cambiar_rol_title))
            .setMessage(getString(msgRes, displayName))
            .setPositiveButton(android.R.string.ok) { _, _ -> applyRoleChange(user, newRole) }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun applyRoleChange(user: AdminUserInfo, newRole: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest.from("users")
                    .update(RoleUpdate(role = newRole)) {
                        filter { eq("id", user.id) }
                    }
                allUsers = allUsers.map { if (it.id == user.id) it.copy(role = newRole) else it }
                applyFilters()
                updateCounters()
                Toast.makeText(requireContext(), getString(R.string.msg_rol_actualizado), Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), getString(R.string.msg_error_cambio_rol), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteUser(user: AdminUserInfo) {
        val displayName = "${user.name} ${user.lastname ?: ""}".trim()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_eliminar_usuario_title))
            .setMessage(getString(R.string.dialog_eliminar_msg, displayName))
            .setPositiveButton(getString(R.string.btn_eliminar)) { _, _ -> deleteUser(user) }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun deleteUser(user: AdminUserInfo) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest.from("users")
                    .delete { filter { eq("id", user.id) } }
                allUsers = allUsers.filter { it.id != user.id }
                applyFilters()
                updateCounters()
                Snackbar.make(requireView(), getString(R.string.msg_usuario_eliminado), Snackbar.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), getString(R.string.msg_error_eliminar_usuario), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class AdminUserAdapter(
    private var users: List<AdminUserInfo>,
    private val currentUserId: String,
    private val onRoleChange: (AdminUserInfo, String) -> Unit,
    private val onDeleteClick: (AdminUserInfo) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tv_avatar_initials)
        val tvName: TextView = view.findViewById(R.id.tv_user_name)
        val tvCorreo: TextView = view.findViewById(R.id.tv_correo)
        val tvRoleBadge: TextView = view.findViewById(R.id.tv_role_badge)
        val btnChangeRole: MaterialButton = view.findViewById(R.id.btn_change_role)
        val btnDelete: MaterialButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val ctx = holder.itemView.context
        val isAdmin = user.role == "admin"
        val isSelf = user.id == currentUserId

        holder.tvInitials.text = buildInitials(user.name, user.lastname)
        holder.tvName.text = "${user.name} ${user.lastname ?: ""}".trim()
        holder.tvCorreo.text = user.correo ?: "—"

        if (isAdmin) {
            val cyan = ctx.getColor(R.color.cyan_primary)
            holder.tvRoleBadge.text = ctx.getString(R.string.label_rol_admin)
            holder.tvRoleBadge.setTextColor(cyan)
            holder.tvRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.argb(50, Color.red(cyan), Color.green(cyan), Color.blue(cyan)))
            holder.btnChangeRole.text = ctx.getString(R.string.btn_degradar_cliente)
            holder.btnChangeRole.strokeColor = ColorStateList.valueOf(ctx.getColor(R.color.alert_red))
            holder.btnChangeRole.setTextColor(ctx.getColor(R.color.alert_red))
        } else {
            val gray = ctx.getColor(R.color.gris_hint)
            holder.tvRoleBadge.text = ctx.getString(R.string.label_rol_cliente)
            holder.tvRoleBadge.setTextColor(gray)
            holder.tvRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.argb(50, Color.red(gray), Color.green(gray), Color.blue(gray)))
            holder.btnChangeRole.text = ctx.getString(R.string.btn_promover_admin)
            holder.btnChangeRole.strokeColor = ColorStateList.valueOf(ctx.getColor(R.color.cyan_primary))
            holder.btnChangeRole.setTextColor(ctx.getColor(R.color.cyan_primary))
        }

        val newRole = if (isAdmin) "client" else "admin"
        holder.btnChangeRole.setOnClickListener { onRoleChange(user, newRole) }

        holder.btnDelete.isEnabled = !isSelf
        holder.btnDelete.alpha = if (isSelf) 0.3f else 1f
        holder.btnDelete.setOnClickListener { onDeleteClick(user) }
    }

    override fun getItemCount() = users.size

    fun updateList(newList: List<AdminUserInfo>) {
        users = newList
        notifyDataSetChanged()
    }

    private fun buildInitials(name: String, lastname: String?): String =
        listOfNotNull(
            name.firstOrNull()?.uppercaseChar()?.toString(),
            lastname?.firstOrNull()?.uppercaseChar()?.toString()
        ).take(2).joinToString("")
}
