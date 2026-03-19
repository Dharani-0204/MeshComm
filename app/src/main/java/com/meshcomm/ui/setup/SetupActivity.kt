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

        val etName    = findViewById<EditText>(R.id.etName)
        val spRole    = findViewById<Spinner>(R.id.spRole)
        val btnStart  = findViewById<Button>(R.id.btnStart)

        // Role spinner
        val roles = arrayOf("Civilian", "Rescuer", "Authority")
        spRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Please enter your name"
                return@setOnClickListener
            }
            val role = when (spRole.selectedItemPosition) {
                1 -> UserRole.RESCUER
                2 -> UserRole.AUTHORITY
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
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) goHome()
        else ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQUEST)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, perms, grants)
        goHome()
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
