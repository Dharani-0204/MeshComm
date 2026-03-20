package com.meshcomm.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.R
import com.meshcomm.databinding.FragmentChatListBinding
import com.meshcomm.ui.home.MeshViewModel

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: PeerListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = PeerListAdapter { peer ->
            // Navigate to chat detail with the selected peer
            val bundle = Bundle().apply {
                putString("userId", peer.deviceId)
                putString("userName", peer.deviceName)
            }
            findNavController().navigate(R.id.chatDetailFragment, bundle)
        }
        
        binding.rvPeers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatListFragment.adapter
        }
    }

    private fun observeData() {
        // Observe connected peers for chat
        viewModel.connectedPeers.asLiveData().observe(viewLifecycleOwner) { peers ->
            adapter.submitList(peers)
            
            if (peers.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvPeers.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvPeers.visibility = View.VISIBLE
            }
        }

        // Update peer count
        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvPeerCount.text = "${stats.connectedCount} nearby users"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}