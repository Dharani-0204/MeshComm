package com.meshcomm.ui.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.meshcomm.R
import com.meshcomm.data.model.MessageType
import com.meshcomm.databinding.ActivityHomeBinding
import com.meshcomm.mesh.MeshService

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    val viewModel: MeshViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigation
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        // Bind to MeshService
        viewModel.bindService(this)

        // Observe mesh stats for status bar
        viewModel.meshStats.observe(this) { stats ->
            binding.tvMeshStatus.text = if (stats.isActive)
                "Mesh active · ${stats.connectedCount} device(s)"
            else
                "Mesh offline"
        }

        // Observe new messages (global snackbar for SOS)
        viewModel.newMessage.observe(this) { msg ->
            msg ?: return@observe
            if (msg.type == MessageType.SOS) {
                Snackbar.make(binding.root, "🚨 SOS from ${msg.senderName}!", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.sos_red))
                    .setTextColor(getColor(android.R.color.white))
                    .setAction("View") {
                        binding.bottomNav.selectedItemId = R.id.nav_sos
                    }.show()
            }
        }

        // Handle deep-link from notification
        intent.getStringExtra("open_tab")?.let { tab ->
            if (tab == "sos") binding.bottomNav.selectedItemId = R.id.nav_sos
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindService(this)
    }
}
