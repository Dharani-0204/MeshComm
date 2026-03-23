package com.meshcomm.ui.home

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.meshcomm.R
import com.meshcomm.data.model.MessageType
import com.meshcomm.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    val viewModel: MeshViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        
        // Connect BottomNavigationView with NavController
        binding.bottomNav.setupWithNavController(navController)
        
        // Set default tab to Map (force navigation on startup)
        binding.bottomNav.selectedItemId = R.id.nav_map

        // Handle back button - proper emergency app behavior with fragment state preservation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (navController.currentDestination?.id) {
                    R.id.nav_map -> {
                        // Already on map (home screen) - exit app
                        finish()
                    }
                    R.id.chatDetailFragment -> {
                        // From chat detail, use proper navigation back to preserve state
                        if (!navController.navigateUp()) {
                            navController.navigate(R.id.nav_chat)
                        }
                    }
                    else -> {
                        // From any other screen, go back to map (emergency default)
                        // Use popBackStack to preserve fragment state if possible
                        if (!navController.popBackStack(R.id.nav_map, false)) {
                            navController.navigate(R.id.nav_map)
                        }
                        binding.bottomNav.selectedItemId = R.id.nav_map
                    }
                }
            }
        })

        // Bind to MeshService
        viewModel.bindService(this)

        // Observe mesh stats for status bar
        viewModel.meshStats.observe(this) { stats ->
            binding.tvMeshStatus.text = if (stats.isActive)
                "Mesh active · ${stats.connectedCount} device(s)"
            else
                "Mesh offline"
            
            // Update status indicator color
            binding.statusDot.setBackgroundResource(
                if (stats.isActive) R.drawable.bg_dot_green else R.drawable.bg_dot_red
            )
        }

        // Observe new SOS messages - show critical alert
        viewModel.newMessage.observe(this) { msg ->
            msg ?: return@observe
            if (msg.type == MessageType.SOS) {
                Snackbar.make(
                    binding.root,
                    "🚨 EMERGENCY: SOS from ${msg.senderName}",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setBackgroundTint(getColor(R.color.sos_red))
                    .setTextColor(getColor(android.R.color.white))
                    .setAction("VIEW") {
                        // Navigate to SOS screen immediately
                        navController.navigate(R.id.nav_sos)
                        binding.bottomNav.selectedItemId = R.id.nav_sos
                    }
                    .show()
            }
        }

        // Handle deep-link from notification
        intent.getStringExtra("open_tab")?.let { tab ->
            when (tab) {
                "sos" -> {
                    navController.navigate(R.id.nav_sos)
                    binding.bottomNav.selectedItemId = R.id.nav_sos
                }
                "map" -> {
                    navController.navigate(R.id.nav_map)
                    binding.bottomNav.selectedItemId = R.id.nav_map
                }
                "mesh" -> {
                    navController.navigate(R.id.nav_mesh)
                    binding.bottomNav.selectedItemId = R.id.nav_mesh
                }
                "chat" -> {
                    navController.navigate(R.id.nav_chat)
                    binding.bottomNav.selectedItemId = R.id.nav_chat
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindService(this)
    }
}
