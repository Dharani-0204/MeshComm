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
import com.meshcomm.databinding.FragmentChatBinding
import com.meshcomm.mesh.PeerRegistry
import com.meshcomm.ui.broadcast.MessageAdapter
import com.meshcomm.ui.home.MeshViewModel
import com.meshcomm.utils.PrefsHelper

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter
    private var targetPeerId: String? = null
    private var targetPeerName: String = "Unknown"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MessageAdapter(requireContext())
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        // Show connected peers to pick
        updatePeerList()

        viewModel.meshStats.observe(viewLifecycleOwner) { updatePeerList() }

        // Load direct messages for selected peer
        targetPeerId?.let { tid ->
            val myId = PrefsHelper.getUserId(requireContext())
            viewModel.allMessages.asLiveData().observe(viewLifecycleOwner) { all ->
                val direct = all.filter { msg ->
                    (msg.senderId == myId && msg.targetId == tid) ||
                    (msg.senderId == tid && msg.targetId == myId)
                }
                adapter.submitList(direct)
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            val tid = targetPeerId
            if (tid == null) {
                Toast.makeText(requireContext(), "Select a peer first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (text.isEmpty()) return@setOnClickListener

            viewModel.sendDirect(tid, targetPeerName, text, binding.switchLocation.isChecked)
            binding.etMessage.setText("")
        }
    }

    private fun updatePeerList() {
        val peers = PeerRegistry.getConnectedPeers()
        if (peers.isEmpty()) {
            binding.tvPeerHint.text = "No peers connected. Searching…"
            binding.tvPeerHint.visibility = View.VISIBLE
        } else {
            binding.tvPeerHint.text = "Connected peers: ${peers.joinToString { it.deviceName }}"
            binding.tvPeerHint.visibility = View.VISIBLE
            // Auto-select first peer for demo
            if (targetPeerId == null) {
                targetPeerId = peers.first().deviceId
                targetPeerName = peers.first().deviceName
                binding.tvChatTitle.text = "Chat with $targetPeerName"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
