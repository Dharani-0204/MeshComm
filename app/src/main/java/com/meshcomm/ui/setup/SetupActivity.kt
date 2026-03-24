package com.meshcomm.ui.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.meshcomm.R
import com.meshcomm.data.model.UserRole
import com.meshcomm.ui.home.HomeActivity
import com.meshcomm.utils.PrefsHelper

class SetupActivity : AppCompatActivity() {

    private val PERM_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val etName = findViewById<EditText>(R.id.etName)
        val actvRole = findViewById<AutoCompleteTextView>(R.id.actvRole)
        val btnStart = findViewById<Button>(R.id.btnStart)

        // Setup Material Design 3 Exposed Dropdown
        val roles = arrayOf("Civilian", "Rescuer", "Volunteer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        actvRole.setAdapter(adapter)
        actvRole.setText(roles[0], false) // Set default value

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Please enter your name"
                return@setOnClickListener
            }
            
            val selectedRole = actvRole.text.toString()
            val role = when (selectedRole) {
                "Rescuer" -> UserRole.RESCUER
                "Volunteer" -> UserRole.AUTHORITY
                else -> UserRole.CIVILIAN
            }
            
            PrefsHelper.setUserName(this, name)
            PrefsHelper.setUserRole(this, role)
            PrefsHelper.setSetupDone(this)
            requestPermissionsAndProceed()
        }
    }

    private fun requestPermissionsAndProceed() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        // Add Bluetooth permissions for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // On older versions, legacy BLUETOOTH and BLUETOOTH_ADMIN are handled via manifest,
            // but we still need location for discovery to work.
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Add background location for zero-intervention mesh when app is minimized
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needed.isEmpty()) goHome()
        else ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQUEST)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, perms, grants)
        goHome() // Proceed anyway, let the app handle missing permissions gracefully
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
