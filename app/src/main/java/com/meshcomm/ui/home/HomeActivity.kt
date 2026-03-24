package com.meshcomm.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.meshcomm.R
import com.meshcomm.data.model.MessageType
import com.meshcomm.databinding.ActivityHomeBinding

/**
 * PRODUCTION-READY HOME ACTIVITY
 * Handles runtime permissions and service binding for the mesh system.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    val viewModel: MeshViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        if (allGranted) {
            viewModel.bindService(this)
        } else {
            Toast.makeText(this, "Permissions required for Mesh functionality", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        checkPermissionsAndStart()

        // Observe mesh stats for status bar
        viewModel.meshStats.observe(this) { stats ->
            binding.tvMeshStatus.text = if (stats.isActive)
                "Mesh active · ${stats.connectedCount} device(s)"
            else
                "Mesh offline"
            
            binding.statusDot.setBackgroundResource(
                if (stats.isActive && stats.connectedCount > 0) R.drawable.bg_dot_green 
                else R.drawable.bg_dot_red
            )
        }

        // Observe new SOS messages
        viewModel.newMessage.observe(this) { msg ->
            msg ?: return@observe
            if (msg.type == MessageType.SOS) {
                showSOSAlert(msg.senderName)
            }
        }
    }

    private fun setupNavigation() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.popBackStack()) finish()
            }
        })
    }

    @SuppressLint("InlinedApi")
    private fun checkPermissionsAndStart() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            viewModel.bindService(this)
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun showSOSAlert(sender: String) {
        Snackbar.make(binding.root, "🚨 EMERGENCY: SOS from $sender", Snackbar.LENGTH_INDEFINITE)
            .setBackgroundTint(getColor(R.color.sos_red))
            .setTextColor(getColor(android.R.color.white))
            .setAction("VIEW") {
                val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
                navHost.navController.navigate(R.id.nav_sos)
                binding.bottomNav.selectedItemId = R.id.nav_sos
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindService(this)
    }
}
