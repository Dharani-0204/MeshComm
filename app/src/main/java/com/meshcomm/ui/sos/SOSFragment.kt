package com.meshcomm.ui.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.databinding.FragmentSosBinding
import com.meshcomm.ui.broadcast.MessageAdapter
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.SpeechToTextHelper

class SOSFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    private var speechHelper: SpeechToTextHelper? = null
    private var vibrator: Vibrator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupVibrator()
        setupRecyclerView()
        setupSpeechToText()
        observeData()
        setupClickListeners()
    }

    private fun setupVibrator() {
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(requireContext())
        binding.rvSosAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true // Show newest first for emergency
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
        // Observe SOS messages
        viewModel.sosMessages.asLiveData().observe(viewLifecycleOwner) { msgs ->
            adapter.submitList(msgs.sortedByDescending { it.timestamp })
            binding.tvSosCount.text = "${msgs.size} SOS alert(s) received"
            
            // Show/hide empty state
            if (msgs.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvSosAlerts.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvSosAlerts.visibility = View.VISIBLE
            }
        }

        // Observe mesh stats
        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvMeshStatus.text = if (stats.isActive) {
                "🟢 Mesh active • ${stats.connectedCount} devices"
            } else {
                "🔴 Mesh offline • Searching..."
            }
        }
    }

    private fun setupClickListeners() {
        // Main SOS button
        binding.btnSOS.setOnClickListener {
            sendSOS(false)
        }

        // Critical SOS (long press)
        binding.btnSOS.setOnLongClickListener {
            sendSOS(true)
            true
        }

        // Speech-to-text button
        binding.btnMic.setOnClickListener {
            binding.btnMic.isEnabled = false
            speechHelper?.startListening()
            Toast.makeText(requireContext(), "🎤 Listening for emergency message...", Toast.LENGTH_SHORT).show()
        }

        // Quick SOS buttons
        binding.btnQuickMedical.setOnClickListener {
            sendQuickSOS("🏥 MEDICAL EMERGENCY - Need immediate medical assistance!")
        }

        binding.btnQuickFire.setOnClickListener {
            sendQuickSOS("🔥 FIRE EMERGENCY - Fire hazard requiring evacuation!")
        }

        binding.btnQuickSafety.setOnClickListener {
            sendQuickSOS("⚠️ SAFETY EMERGENCY - Dangerous situation requiring help!")
        }
    }

    private fun sendSOS(isCritical: Boolean) {
        val customMessage = binding.etSosMessage.text.toString().trim()
        val message = when {
            customMessage.isNotEmpty() -> customMessage
            isCritical -> "🚨 CRITICAL EMERGENCY - Immediate assistance required!"
            else -> "🆘 EMERGENCY - I need help!"
        }

        // Get location if available
        val location = getCurrentLocation()
        val locationText = if (location != null) {
            " Location: ${location.latitude}, ${location.longitude}"
        } else {
            " Location: Unknown"
        }

        val fullMessage = message + locationText

        // Send SOS
        viewModel.sendSOS(fullMessage)

        // Show confirmation
        val confirmationText = if (isCritical) "CRITICAL SOS SENT!" else "SOS SENT!"
        Toast.makeText(requireContext(), confirmationText, Toast.LENGTH_LONG).show()

        // Haptic feedback
        vibrateEmergency(isCritical)

        // Clear input
        binding.etSosMessage.setText("")

        // Show success animation
        binding.confirmationText.text = "✅ $confirmationText"
        binding.confirmationText.visibility = View.VISIBLE
        binding.confirmationText.postDelayed({
            binding.confirmationText.visibility = View.GONE
        }, 3000)
    }

    private fun sendQuickSOS(message: String) {
        val location = getCurrentLocation()
        val locationText = if (location != null) {
            " Location: ${location.latitude}, ${location.longitude}"
        } else {
            " Location: Unknown"
        }

        viewModel.sendSOS(message + locationText)
        Toast.makeText(requireContext(), "Emergency alert sent!", Toast.LENGTH_LONG).show()
        vibrateEmergency(false)
    }

    private fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }

    private fun vibrateEmergency(isCritical: Boolean) {
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val pattern = if (isCritical) {
                    // Strong vibration pattern for critical
                    VibrationEffect.createWaveform(longArrayOf(0, 500, 100, 500, 100, 500), -1)
                } else {
                    // Single strong vibration for normal SOS
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator?.vibrate(pattern)
            } else {
                // Fallback for older devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isCritical) 1000 else 200)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechHelper?.destroy()
        _binding = null
    }
}
