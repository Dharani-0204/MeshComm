package com.meshcomm.ui.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.databinding.FragmentMeshBinding
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.SpeechToTextHelper

class BroadcastFragment : Fragment() {

    private var _binding: FragmentMeshBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    private var speechHelper: SpeechToTextHelper? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentMeshBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupSpeechToText()
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
        // Observe broadcast messages
        viewModel.broadcastMessages.asLiveData().observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
                binding.emptyState.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.VISIBLE
            }
        }

        // Observe mesh statistics
        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvConnectedDevices.text = "${stats.connectedCount} devices connected"
            binding.tvMeshStatus.text = if (stats.isActive) "Mesh Network Active" else "Searching for mesh..."
        }
    }

    private fun setupClickListeners() {
        // Send button
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                val includeLocation = binding.switchLocation.isChecked
                viewModel.sendBroadcast(message, includeLocation)
                binding.etMessage.setText("")
                Toast.makeText(requireContext(), "Message broadcasted", Toast.LENGTH_SHORT).show()
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
                "GPS location will be included"
            } else {
                "Messages without location"
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
