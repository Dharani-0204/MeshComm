package com.meshcomm.ui.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.meshcomm.R
import com.meshcomm.data.model.UserRole
import com.meshcomm.ui.home.HomeActivity
import com.meshcomm.utils.PrefsHelper

class SetupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SetupActivity"
        private const val PERM_REQUEST = 100
    }

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

            Log.d(TAG, "User setup: name=$name, role=$role")

            // For civilians, show profile prompt
            if (role == UserRole.CIVILIAN) {
                showProfilePromptDialog()
            } else {
                // Rescuers skip profile, mark as complete
                PrefsHelper.setProfileComplete(this, true)
                PrefsHelper.setSetupDone(this)
                requestPermissionsAndProceed()
            }
        }
    }

    private fun showProfilePromptDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_prompt, null)

        val tilBloodGroup = dialogView.findViewById<TextInputLayout>(R.id.tilBloodGroup)
        val actvBloodGroup = dialogView.findViewById<AutoCompleteTextView>(R.id.actvBloodGroup)
        val tilMedicalConditions = dialogView.findViewById<TextInputLayout>(R.id.tilMedicalConditions)
        val etMedicalConditions = dialogView.findViewById<TextInputEditText>(R.id.etMedicalConditions)
        val tilAllergies = dialogView.findViewById<TextInputLayout>(R.id.tilAllergies)
        val etAllergies = dialogView.findViewById<TextInputEditText>(R.id.etAllergies)

        // Setup blood group dropdown
        val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")
        val bloodAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodGroups)
        actvBloodGroup.setAdapter(bloodAdapter)

        AlertDialog.Builder(this)
            .setTitle("Emergency Medical Profile")
            .setMessage("This information helps rescuers during emergencies. You can complete it now or later.")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val bloodGroup = actvBloodGroup.text.toString()
                val conditions = etMedicalConditions.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val allergies = etAllergies.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                // Save profile data
                PrefsHelper.setBloodGroup(this, bloodGroup)
                PrefsHelper.setMedicalConditions(this, conditions)
                PrefsHelper.setAllergies(this, allergies)

                // Check if profile is complete
                val isComplete = bloodGroup.isNotEmpty() && bloodGroup != "Unknown"
                PrefsHelper.setProfileComplete(this, isComplete)
                PrefsHelper.setProfileSkipped(this, false)
                PrefsHelper.setSetupDone(this)

                Log.d(TAG, "Profile saved: bloodGroup=$bloodGroup, complete=$isComplete")
                requestPermissionsAndProceed()
            }
            .setNegativeButton("Skip for now") { _, _ ->
                PrefsHelper.setProfileSkipped(this, true)
                PrefsHelper.setProfileComplete(this, false)
                PrefsHelper.setSetupDone(this)
                Log.d(TAG, "Profile skipped")
                requestPermissionsAndProceed()
            }
            .setCancelable(false)
            .show()
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
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) goHome()
        else ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQUEST)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, perms, grants)

        // Check if all permissions were granted
        if (grants.isNotEmpty() && grants.all { it == PackageManager.PERMISSION_GRANTED }) {
            goHome()
        } else {
            // Show dialog explaining why permissions are needed
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("MeshComm needs Bluetooth and Location permissions to connect with nearby devices. Without these, the app cannot function.")
                .setPositiveButton("Grant Permissions") { _, _ -> requestPermissionsAndProceed() }
                .setNegativeButton("Exit") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
