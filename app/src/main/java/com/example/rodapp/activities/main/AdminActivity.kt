package com.example.rodapp.activities.main

import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.rodapp.R
import com.example.rodapp.fragments.AdminDashboardFragment
import com.example.rodapp.fragments.AdminUsersFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val toolbar = findViewById<MaterialToolbar>(R.id.admin_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.admin_bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_admin_dashboard

        if (savedInstanceState == null) {
            loadFragment(AdminDashboardFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_dashboard -> { loadFragment(AdminDashboardFragment()); true }
                R.id.nav_admin_users -> { loadFragment(AdminUsersFragment()); true }
                R.id.nav_admin_activity -> { loadFragment(AdminActivityStubFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }
}

class AdminActivityStubFragment : androidx.fragment.app.Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val view = inflater.inflate(R.layout.fragment_admin_activity_stub, container, false)
        return view
    }
}
