package com.meshcomm.ui.broadcast

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.databinding.FragmentBroadcastBinding
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.MediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * BroadcastFragment: Handles text, audio, and image messaging over the mesh network.
 * Corrected for permissions, background processing, and lifecycle safety.
 */
class BroadcastFragment : Fragment() {

    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    
    private var isRecording = false
    private var recordingStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var lastRecordedPath: String? = null

    // 1. IMAGE PICKER & COMPRESSION (OPTIMIZED)
    // FIX: Moved compression to IO thread to avoid blocking UI
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val compressedPath = MediaHelper.compressImage(requireContext(), it)
                withContext(Dispatchers.Main) {
                    if (compressedPath != null) {
                        val file = File(compressedPath)
                        // FIX: Added file size validation (Max 500KB)
                        if (file.length() <= 500 * 1024) {
                            viewModel.sendImage(compressedPath)
                            Toast.makeText(requireContext(), "Image sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Image too large (>500KB)", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Image compression failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 2. PERMISSION HANDLER (NEW)
    // FIX: Proper runtime permission handling for Audio
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(requireContext(), "Mic permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        // FIX: Ensure correct binding is used
        _binding = FragmentBroadcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeMessages()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(requireContext())
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = this@BroadcastFragment.adapter
        }
    }

    private fun observeMessages() {
        // Observe broadcast messages from Flow via LiveData
        viewModel.broadcastMessages.asLiveData().observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        // Observe mesh stats for peer count
        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvPeerCountHeader.text = String.format(Locale.getDefault(), "%d peers", stats.connectedCount)
        }
    }

    private fun setupClickListeners() {
        // TEXT: Send broadcast
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                val includeLocation = binding.switchLocation.isChecked
                viewModel.sendBroadcast(message, includeLocation)
                binding.etMessage.setText("")
            }
        }

        // AUDIO: Check permission and toggle recording
        binding.btnMic.setOnClickListener {
            if (!isRecording) {
                checkAudioPermissionAndRecord()
            } else {
                stopRecording()
            }
        }

        // IMAGE: Open picker
        binding.btnImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun checkAudioPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 3. AUDIO RECORDING (FIXED LOGIC)
    private fun startRecording() {
        val fileName = "rec_${System.currentTimeMillis()}"
        try {
            val path = MediaHelper.startRecording(requireContext(), fileName)
            if (path != null) {
                isRecording = true
                recordingStartTime = System.currentTimeMillis()
                lastRecordedPath = path
                
                // UI: Update state
                binding.layoutRecording.visibility = View.VISIBLE
                // FIX: Use setIconResource for MaterialButton
                binding.btnMic.setIconResource(android.R.drawable.ic_media_pause)
                
                updateRecordingTimer()
                Toast.makeText(requireContext(), "Recording...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Recorder failed to start", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Recording error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        val duration = System.currentTimeMillis() - recordingStartTime
        
        MediaHelper.stopRecording()
        
        // UI: Reset state
        binding.layoutRecording.visibility = View.GONE
        binding.btnMic.setIconResource(android.R.drawable.ic_btn_speak_now)
        // FIX: Clear handler to prevent leaks and stale updates
        handler.removeCallbacksAndMessages(null)
        
        lastRecordedPath?.let { path ->
            val file = File(path)
            if (file.exists() && duration > 1000) { // Min 1s validation
                // FIX: Added file size validation (1MB)
                if (file.length() <= 1024 * 1024) {
                    viewModel.sendAudio(path, duration)
                    Toast.makeText(requireContext(), "Audio sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Audio too large (>1MB)", Toast.LENGTH_LONG).show()
                }
            } else if (duration <= 1000) {
                Toast.makeText(requireContext(), "Recording too short", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 4. TIMER & MEMORY LEAK PREVENTION
    private fun updateRecordingTimer() {
        // FIX: Check if binding is null to prevent crashes during lifecycle transitions
        if (!isRecording || _binding == null) return
        
        val elapsed = System.currentTimeMillis() - recordingStartTime
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / (1000 * 60)) % 60
        binding.tvRecordDuration.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        
        handler.postDelayed({ updateRecordingTimer() }, 1000)
    }

    override fun onDestroyView() {
        // FIX: Ensure resources are released on destroy
        if (isRecording) {
            MediaHelper.stopRecording()
        }
        handler.removeCallbacksAndMessages(null) // FIX: Prevent memory leak
        _binding = null
        super.onDestroyView()
    }
}
