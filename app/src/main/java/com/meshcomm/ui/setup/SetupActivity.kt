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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Skip setup if already done
        if (PrefsHelper.isSetupDone(this)) {
            goHome()
            return
        }

        setContentView(R.layout.activity_setup)

        val etName = findViewById<EditText>(R.id.etName)
        val actvRole = findViewById<AutoCompleteTextView>(R.id.actvRole)
        val btnStart = findViewById<Button>(R.id.btnStart)

        // ✅ Role dropdown (clean + consistent with your logic)
        val roles = arrayOf("Civilian", "Rescuer", "Authority")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        actvRole.setAdapter(adapter)
        actvRole.setText(roles[0], false)

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Please enter your name"
                return@setOnClickListener
            }

            val selectedRole = actvRole.text.toString()
            val role = when (selectedRole) {
                "Authority" -> UserRole.AUTHORITY
                "Rescuer" -> UserRole.RESCUER
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
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            goHome()
        } else {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, perms, grants)

        if (code == PERMISSION_REQUEST_CODE) {
            if (grants.isNotEmpty() && grants.all { it == PackageManager.PERMISSION_GRANTED }) {
                goHome()
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("MeshComm needs Bluetooth and Location permissions to connect with nearby devices.")
                    .setPositiveButton("Grant Permissions") { _, _ -> requestPermissionsAndProceed() }
                    .setNegativeButton("Exit") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
