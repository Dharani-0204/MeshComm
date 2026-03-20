package com.meshcomm.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.databinding.FragmentChatDetailBinding
import com.meshcomm.ui.broadcast.MessageAdapter
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.PrefsHelper
import com.meshcomm.utils.SpeechToTextHelper

class ChatDetailFragment : Fragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    private var speechHelper: SpeechToTextHelper? = null
    
    private var userId: String? = null
    private var userName: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Get arguments
        userId = arguments?.getString("userId")
        userName = arguments?.getString("userName")
        
        setupHeader()
        setupRecyclerView()
        setupSpeechToText()
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

    private fun setupSpeechToText() {
        speechHelper = SpeechToTextHelper(
            activity = requireActivity(),
            onResult = { text ->
                binding.etMessage.setText(text)
                binding.btnMic.isEnabled = true
                Toast.makeText(requireContext(), "Speech recognized!", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                binding.btnMic.isEnabled = true
                Toast.makeText(requireContext(), "Speech error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun observeMessages() {
        val targetId = userId ?: return
        val myId = PrefsHelper.getUserId(requireContext())

        // Filter messages for this specific chat
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
        // Send button
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            val targetId = userId
            
            if (targetId == null) {
                Toast.makeText(requireContext(), "Invalid chat target", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (message.isNotEmpty()) {
                val includeLocation = binding.switchLocation.isChecked
                viewModel.sendDirect(targetId, userName ?: "Unknown", message, includeLocation)
                binding.etMessage.setText("")
                Toast.makeText(requireContext(), "Message sent", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }

        // Microphone button
        binding.btnMic.setOnClickListener {
            binding.btnMic.isEnabled = false
            speechHelper?.startListening()
            Toast.makeText(requireContext(), "🎤 Listening...", Toast.LENGTH_SHORT).show()
        }

        // Location switch
        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) {
                "GPS location will be included with messages"
            } else {
                "Messages will not include location"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechHelper?.destroy()
        _binding = null
    }
}