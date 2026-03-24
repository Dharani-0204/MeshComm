package com.meshcomm.ui.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.databinding.FragmentSosBinding
import com.meshcomm.location.LocationProvider
import com.meshcomm.ui.broadcast.MessageAdapter
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.EmergencyContactsManager
import com.meshcomm.utils.GeocoderUtil
import com.meshcomm.utils.PrefsHelper
import com.meshcomm.utils.SmsHelper
import com.meshcomm.utils.SpeechToTextHelper

data class EmergencyContact(val name: String, val phone: String)

class SOSFragment : Fragment() {

    companion object {
        private const val TAG = "SOSFragment"
        private const val REQUEST_READ_CONTACTS = 101
        private const val REQUEST_SMS_PERMISSION = 201
    }

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    private var speechHelper: SpeechToTextHelper? = null
    private var vibrator: Vibrator? = null
    private var selectedEmergencyContacts = mutableListOf<EmergencyContact>()
    private var lastSentLocation: Location? = null

    // Track SMS results
    private val smsResults = mutableListOf<SmsHelper.SmsResult>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        Log.d(TAG, "SOSFragment onCreateView")
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "SOSFragment onViewCreated")
        setupVibrator()
        setupRecyclerView()
        setupSpeechToText()
        observeData()
        setupClickListeners()
        checkSmsPermission()
    }

    private fun checkSmsPermission() {
        if (!SmsHelper.hasSmsPermission(requireContext())) {
            Log.d(TAG, "SMS permission not granted - will request when needed")
        }
    }

    private fun setupVibrator() {
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        Log.d(TAG, "Vibrator initialized: hasVibrator=${vibrator?.hasVibrator()}")
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(requireContext())
        binding.rvSosAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true
                stackFromEnd = false
            }
            adapter = this@SOSFragment.adapter
        }
    }

    private fun setupSpeechToText() {
        speechHelper = SpeechToTextHelper(
            activity = requireActivity(),
            onResult = { text ->
                binding.etSosMessage.setText(text)
                binding.btnMic.isEnabled = true
                Toast.makeText(requireContext(), "Speech recognized!", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                binding.btnMic.isEnabled = true
                Toast.makeText(requireContext(), "Speech error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun observeData() {
        viewModel.sosMessages.asLiveData().observe(viewLifecycleOwner) { msgs ->
            adapter.submitList(msgs.sortedByDescending { it.timestamp })
            binding.tvSosCount.text = "${msgs.size} SOS alert(s) received"

            if (msgs.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvSosAlerts.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvSosAlerts.visibility = View.VISIBLE
            }
        }

        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvMeshStatus.text = if (stats.isActive) {
                "Mesh active - ${stats.connectedCount} devices"
            } else {
                "Mesh offline - Searching..."
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSOS.setOnClickListener {
            sendSOS(false)
        }

        binding.btnSOS.setOnLongClickListener {
            sendSOS(true)
            true
        }

        binding.btnMic.setOnClickListener {
            binding.btnMic.isEnabled = false
            speechHelper?.startListening()
            Toast.makeText(requireContext(), "Listening for emergency message...", Toast.LENGTH_SHORT).show()
        }

        binding.btnQuickMedical.setOnClickListener {
            sendQuickSOS("MEDICAL EMERGENCY - Need immediate medical assistance!")
        }

        binding.btnQuickFire.setOnClickListener {
            sendQuickSOS("FIRE EMERGENCY - Fire hazard requiring evacuation!")
        }

        binding.btnQuickSafety.setOnClickListener {
            sendQuickSOS("SAFETY EMERGENCY - Dangerous situation requiring help!")
        }
    }

    private fun sendSOS(isCritical: Boolean) {
        Log.d(TAG, "sendSOS called - isCritical: $isCritical")
        val customMessage = binding.etSosMessage.text.toString().trim()
        val message = when {
            customMessage.isNotEmpty() -> customMessage
            isCritical -> "CRITICAL EMERGENCY - Immediate assistance required!"
            else -> "EMERGENCY - I need help!"
        }

        // Show getting location indicator
        showLocationStatus("Getting location...")

        // Request fresh location with callback
        requestFreshLocationForSOS(message, isCritical)
    }

    private fun sendQuickSOS(message: String) {
        showLocationStatus("Getting location...")
        requestFreshLocationForSOS(message, false)
    }

    private fun requestFreshLocationForSOS(message: String, isCritical: Boolean) {
        // First try to get cached location for immediate response
        val cachedLocation = getCurrentLocation()
        if (cachedLocation != null) {
            Log.d(TAG, "Using cached location for immediate SOS")
            lastSentLocation = cachedLocation
            hideLocationStatus()
            executeSOS(message, isCritical)
            return
        }

        // No cached location available, request fresh one
        Log.d(TAG, "No cached location, requesting fresh location for SOS")

        // Create a simple LocationProvider instance and use callback
        val locationProvider = com.meshcomm.location.LocationProvider(requireContext())

        locationProvider.requestFreshLocation(object : com.meshcomm.location.LocationProvider.LocationCallback {
            override fun onLocationReceived(location: Location) {
                Log.d(TAG, "Fresh location received for SOS: ${location.latitude}, ${location.longitude}")
                lastSentLocation = location

                activity?.runOnUiThread {
                    hideLocationStatus()
                    executeSOS(message, isCritical)
                }
            }

            override fun onLocationFailed(error: String) {
                Log.w(TAG, "Fresh location failed for SOS: $error")

                activity?.runOnUiThread {
                    hideLocationStatus()

                    // Show error but still allow SOS to be sent without location
                    Toast.makeText(
                        requireContext(),
                        "Location unavailable: $error\nSending SOS without location...",
                        Toast.LENGTH_SHORT
                    ).show()

                    lastSentLocation = null
                    executeSOS(message, isCritical)
                }
            }
        })
    }

    private fun showLocationStatus(message: String) {
        // Show location status in the confirmation text area temporarily
        binding.confirmationText.text = message
        binding.confirmationText.visibility = View.VISIBLE
    }

    private fun hideLocationStatus() {
        // Only hide if it's showing a location status message
        if (binding.confirmationText.text.toString().contains("location", ignoreCase = true)) {
            binding.confirmationText.visibility = View.GONE
        }
    }

    private fun executeSOS(message: String, isCritical: Boolean) {
        Log.i(TAG, "Executing SOS - sending via mesh and SMS")

        // 1. Send via Bluetooth Mesh Network
        val locationText = lastSentLocation?.let { loc ->
            " Location: ${loc.latitude}, ${loc.longitude}"
        } ?: " Location: Unknown"
        val fullMessage = message + locationText

        viewModel.sendSOS(fullMessage)
        Log.d(TAG, "SOS sent to mesh network: $fullMessage")

        // 2. Send SMS to all saved emergency contacts
        sendSmsToEmergencyContacts(message)

        // 3. Show confirmation
        showSosConfirmation(isCritical)
        binding.etSosMessage.setText("")
    }

    private fun sendSmsToEmergencyContacts(message: String) {
        val savedContacts = EmergencyContactsManager.getContacts(requireContext())

        if (savedContacts.isEmpty()) {
            Log.w(TAG, "No saved emergency contacts for SMS")
            return
        }

        // Check SMS permission
        if (!SmsHelper.hasSmsPermission(requireContext())) {
            Log.w(TAG, "SMS permission not granted - requesting")
            SmsHelper.requestSmsPermission(requireActivity())
            Toast.makeText(
                requireContext(),
                "SMS permission needed to alert contacts",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Build SMS message with location
        val senderName = PrefsHelper.getUserName(requireContext())
        val (lat, lon) = lastSentLocation?.let { Pair(it.latitude, it.longitude) }
            ?: Pair(0.0, 0.0)
        val address = if (lat != 0.0 || lon != 0.0) {
            GeocoderUtil.getShortAddress(requireContext(), lat, lon)
        } else null

        val smsMessage = SmsHelper.buildSosMessage(
            senderName = senderName,
            latitude = lat,
            longitude = lon,
            address = address,
            customMessage = message
        )

        Log.i(TAG, "Sending SMS to ${savedContacts.size} emergency contacts")
        smsResults.clear()

        val phoneNumbers = savedContacts.map { it.phone }
        SmsHelper.sendSmsToMultiple(
            context = requireContext(),
            phoneNumbers = phoneNumbers,
            message = smsMessage,
            callback = object : SmsHelper.SmsCallback {
                override fun onSmsSent(result: SmsHelper.SmsResult) {
                    smsResults.add(result)
                    val status = if (result.success) "SENT" else "FAILED"
                    Log.d(TAG, "SMS to ${result.phoneNumber}: $status")
                }

                override fun onAllSmsCompleted(results: List<SmsHelper.SmsResult>) {
                    val successCount = results.count { it.success }
                    val failCount = results.size - successCount
                    Log.i(TAG, "SMS completed: $successCount sent, $failCount failed")

                    activity?.runOnUiThread {
                        if (failCount > 0) {
                            Toast.makeText(
                                requireContext(),
                                "SMS: $successCount sent, $failCount failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )
    }

    private fun showSosConfirmation(isCritical: Boolean) {
        val confirmationText = if (isCritical) "CRITICAL SOS SENT!" else "SOS SENT!"
        Log.i(TAG, "SOS confirmation: $confirmationText")
        Toast.makeText(requireContext(), confirmationText, Toast.LENGTH_LONG).show()

        vibrateEmergency(isCritical)

        binding.confirmationText.text = confirmationText
        binding.confirmationText.visibility = View.VISIBLE

        // Show summary dialog
        showSummaryDialog()

        binding.confirmationText.postDelayed({
            binding.confirmationText.visibility = View.GONE
        }, 3000)

        lastSentLocation = null
    }

    private fun showSummaryDialog() {
        val savedContacts = EmergencyContactsManager.getContacts(requireContext())
        val meshStatus = viewModel.meshStats.value

        val addressText = lastSentLocation?.let { loc ->
            val address = GeocoderUtil.getShortAddress(requireContext(), loc.latitude, loc.longitude)
            "Location: $address"
        } ?: "Location: Unknown"

        val meshInfo = if (meshStatus?.isActive == true) {
            "Mesh: Sent to ${meshStatus.connectedCount} devices"
        } else {
            "Mesh: Offline (will send when connected)"
        }

        val smsInfo = if (savedContacts.isNotEmpty()) {
            val contactsList = savedContacts.joinToString("\n") { "  - ${it.name} (${it.phone})" }
            "SMS sent to ${savedContacts.size} contacts:\n$contactsList"
        } else {
            "SMS: No emergency contacts saved\n(Add contacts in Profile tab)"
        }

        val message = """
            |$addressText
            |
            |$meshInfo
            |
            |$smsInfo
        """.trimMargin()

        AlertDialog.Builder(requireContext())
            .setTitle("SOS Alert Sent")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()

        Log.i(TAG, "SOS Summary:\n$message")
    }

    private fun getCurrentLocation(): Location? {
        Log.d(TAG, "Getting current location (fallback method)")
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            // Try to get the best available location
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Prefer GPS if available and recent, otherwise use network
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    // Use more recent location, prefer GPS if times are close
                    if (gpsLocation.time > networkLocation.time - 30000) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

            if (bestLocation != null) {
                Log.d(TAG, "Location obtained: (${bestLocation.latitude}, ${bestLocation.longitude}) from ${
                    if (bestLocation == gpsLocation) "GPS" else "Network"
                }")
            } else {
                Log.w(TAG, "No location available from any provider")
            }
            bestLocation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}", e)
            null
        }
    }

    private fun vibrateEmergency(isCritical: Boolean) {
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = if (isCritical) {
                    VibrationEffect.createWaveform(longArrayOf(0, 500, 100, 500, 100, 500), -1)
                } else {
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator?.vibrate(pattern)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isCritical) 1000 else 200)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Contacts permission granted", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_SMS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "SMS permission granted", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "SMS permission granted")
                } else {
                    Toast.makeText(requireContext(), "SMS permission denied - cannot send SMS alerts", Toast.LENGTH_LONG).show()
                    Log.w(TAG, "SMS permission denied")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "SOSFragment onDestroyView")
        speechHelper?.destroy()
        _binding = null
    }
}
