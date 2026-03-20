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
import com.meshcomm.databinding.FragmentBroadcastBinding
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.SpeechToTextHelper

class BroadcastFragment : Fragment() {

    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    private var speechHelper: SpeechToTextHelper? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentBroadcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MessageAdapter(requireContext())
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        // Initialize speech-to-text helper
        speechHelper = SpeechToTextHelper(
            activity = requireActivity(),
            onResult = { text ->
                binding.etMessage.setText(text)
                binding.btnMic.isEnabled = true
                Toast.makeText(requireContext(), "Speech recognized!", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                binding.btnMic.isEnabled = true
                Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )

        viewModel.broadcastMessages.asLiveData().observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) binding.rvMessages.scrollToPosition(messages.size - 1)
        }

        binding.switchLocation.setOnCheckedChangeListener { _, _ -> }

        // Mic button - start speech recognition
        binding.btnMic.setOnClickListener {
            binding.btnMic.isEnabled = false
            speechHelper?.startListening()
            Toast.makeText(requireContext(), "🎤 Listening...", Toast.LENGTH_SHORT).show()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendBroadcast(text, binding.switchLocation.isChecked)
                binding.etMessage.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechHelper?.destroy()
        _binding = null
    }
}
