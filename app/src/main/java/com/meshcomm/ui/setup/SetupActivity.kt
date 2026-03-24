package com.meshcomm.ui.setup

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.meshcomm.R
import com.meshcomm.data.model.UserRole
import com.meshcomm.ui.home.HomeActivity
import com.meshcomm.utils.PrefsHelper

class SetupActivity : AppCompatActivity() {

    private val TAG = "SetupActivity"
    private val PERM_REQUEST = 100
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Bluetooth enabled")
            checkWifiAndProceed()
        } else {
            Toast.makeText(this, "Bluetooth is required for MeshComm", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val etName = findViewById<EditText>(R.id.etName)
        val actvRole = findViewById<AutoCompleteTextView>(R.id.actvRole)
        val btnStart = findViewById<Button>(R.id.btnStart)

        val roles = arrayOf("Civilian", "Rescuer", "Volunteer")
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
        if (needed.isEmpty()) {
            checkHardwareAndProceed()
        } else {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQUEST)
        }
    }

    private fun checkHardwareAndProceed() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
            checkWifiAndProceed()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        } else {
            checkWifiAndProceed()
        }
    }

    private fun checkWifiAndProceed() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable WiFi for WiFi Direct mesh networking", Toast.LENGTH_LONG).show()
            val wifiIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            startActivity(wifiIntent)
            // Note: We don't block here because BT might still work, 
            // but we'll re-check next time discovery runs.
        }
        goHome()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, perms, grants)
        if (grants.isNotEmpty() && grants.all { it == PackageManager.PERMISSION_GRANTED }) {
            checkHardwareAndProceed()
        } else {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show()
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
