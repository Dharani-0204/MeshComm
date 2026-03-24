package com.meshcomm.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import com.meshcomm.databinding.FragmentChatDetailBinding
import com.meshcomm.ui.broadcast.MessageAdapter
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.MediaHelper
import com.meshcomm.utils.PrefsHelper
import com.meshcomm.utils.SpeechToTextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class ChatDetailFragment : Fragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    
    private var userId: String? = null
    private var userName: String? = null

    private var isRecording = false
    private var recordingStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var lastRecordedPath: String? = null

    // Image Picker
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val compressedPath = MediaHelper.compressImage(requireContext(), it)
                withContext(Dispatchers.Main) {
                    if (compressedPath != null) {
                        viewModel.sendImage(compressedPath, userId)
                        Toast.makeText(requireContext(), "Image sent", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Audio Permission
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startRecording()
        else Toast.makeText(requireContext(), "Mic permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userId = arguments?.getString("userId")
        userName = arguments?.getString("userName")
        
        setupHeader()
        setupRecyclerView()
        observeMessages()
        setupClickListeners()
    }

    private fun setupHeader() {
        binding.tvChatTitle.text = userName ?: "Unknown User"
        binding.tvUserId.text = "ID: ${userId?.take(8) ?: "Unknown"}"
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(requireContext())
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = this@ChatDetailFragment.adapter
        }
    }

    private fun observeMessages() {
        val targetId = userId ?: return
        val myId = PrefsHelper.getUserId(requireContext())

        viewModel.allMessages.asLiveData().observe(viewLifecycleOwner) { allMessages ->
            val chatMessages = allMessages.filter { msg ->
                (msg.senderId == myId && msg.targetId == targetId) ||
                (msg.senderId == targetId && msg.targetId == myId)
            }.sortedBy { it.timestamp }
            
            adapter.submitList(chatMessages)
            if (chatMessages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(chatMessages.size - 1)
                binding.emptyState.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty() && userId != null) {
                viewModel.sendDirect(userId!!, userName ?: "", message, binding.switchLocation.isChecked)
                binding.etMessage.setText("")
            }
        }

        binding.btnMic.setOnClickListener {
            if (!isRecording) checkAudioPermissionAndRecord()
            else stopRecording()
        }

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

    private fun startRecording() {
        val path = MediaHelper.startRecording(requireContext(), "chat_rec_${System.currentTimeMillis()}")
        if (path != null) {
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            lastRecordedPath = path
            binding.layoutRecording.visibility = View.VISIBLE
            updateRecordingTimer()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        val duration = System.currentTimeMillis() - recordingStartTime
        MediaHelper.stopRecording()
        binding.layoutRecording.visibility = View.GONE
        handler.removeCallbacksAndMessages(null)
        
        lastRecordedPath?.let { path ->
            if (File(path).exists() && duration > 1000) {
                viewModel.sendAudio(path, duration, userId)
            }
        }
    }

    private fun updateRecordingTimer() {
        if (!isRecording || _binding == null) return
        val elapsed = System.currentTimeMillis() - recordingStartTime
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / (1000 * 60)) % 60
        binding.tvRecordDuration.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        handler.postDelayed({ updateRecordingTimer() }, 1000)
    }

    override fun onDestroyView() {
        if (isRecording) MediaHelper.stopRecording()
        handler.removeCallbacksAndMessages(null)
        _binding = null
        super.onDestroyView()
    }
}
