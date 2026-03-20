package com.meshcomm.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.R
import com.meshcomm.databinding.FragmentMapBinding
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.ui.home.MeshViewModel

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var sosListAdapter: SosListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        sosListAdapter = SosListAdapter { sosMessage ->
            // Handle SOS pin tap - could show details or navigate
            Toast.makeText(requireContext(), 
                "SOS from ${sosMessage.senderName}: ${sosMessage.content}", 
                Toast.LENGTH_LONG).show()
        }
        binding.rvSosList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sosListAdapter
        }
    }

    private fun setupClickListeners() {
        // Floating SOS button
        binding.fabSos.setOnClickListener {
            // Navigate to SOS screen
            findNavController().navigate(R.id.nav_sos)
        }

        // Emergency zone button
        binding.btnEmergencyZones.setOnClickListener {
            showEmergencyZones()
        }
    }

    private fun observeData() {
        // Observe all messages to display location data
        viewModel.allMessages.asLiveData().observe(viewLifecycleOwner) { allMessages ->
            val messagesWithLocation = allMessages.filter { 
                it.latitude != 0.0 && it.longitude != 0.0 
            }
            val sosList = messagesWithLocation.filter { it.type == MessageType.SOS }
            val normalMessages = messagesWithLocation.filter { it.type == MessageType.BROADCAST || it.type == MessageType.DIRECT }

            // Update SOS list
            sosListAdapter.submitList(sosList.sortedByDescending { it.timestamp })
            
            // Update statistics
            binding.tvMapInfo.text = when {
                sosList.isNotEmpty() -> "🚨 ${sosList.size} SOS alerts • ${normalMessages.size} messages with location"
                normalMessages.isNotEmpty() -> "📍 ${normalMessages.size} messages with location"
                else -> "No location data received yet"
            }

            // Update emergency zones
            if (sosList.isNotEmpty()) {
                binding.btnEmergencyZones.visibility = View.VISIBLE
                binding.tvEmergencyZone.visibility = View.VISIBLE
                binding.tvEmergencyZone.text = "⚠️ ${sosList.size} emergency zone(s) detected"
            } else {
                binding.btnEmergencyZones.visibility = View.GONE
                binding.tvEmergencyZone.visibility = View.GONE
            }

            // Draw location overview
            drawLocationOverview(messagesWithLocation, sosList)
            
            // Show/hide empty state
            if (messagesWithLocation.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.locationContent.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.locationContent.visibility = View.VISIBLE
            }
        }

        // Observe mesh statistics
        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvMeshInfo.text = if (stats.isActive) {
                "🟢 Mesh active • ${stats.connectedCount} devices • Location sharing enabled"
            } else {
                "🔴 Mesh offline • Searching for connections..."
            }
        }
    }

    private fun drawLocationOverview(allMessages: List<Message>, sosMessages: List<Message>) {
        if (allMessages.isEmpty()) {
            binding.tvLocationOverview.text = "No location data available.\nMessages with GPS enabled will appear here."
            return
        }

        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("=== Location Overview ===\n")
        
        // Show SOS messages first (priority)
        if (sosMessages.isNotEmpty()) {
            stringBuilder.appendLine("🚨 EMERGENCY ALERTS:")
            sosMessages.take(5).forEach { sos ->
                stringBuilder.appendLine("  • ${sos.senderName}: ${sos.content.take(40)}")
                stringBuilder.appendLine("    📍 ${String.format("%.4f", sos.latitude)}, ${String.format("%.4f", sos.longitude)}")
                stringBuilder.appendLine("    🔋 ${sos.batteryLevel}% battery • ${formatTimestamp(sos.timestamp)}")
                stringBuilder.appendLine()
            }
        }
        
        // Show normal messages
        val normalMessages = allMessages.filter { it.type == MessageType.BROADCAST || it.type == MessageType.DIRECT }
        if (normalMessages.isNotEmpty()) {
            stringBuilder.appendLine("📍 LOCATION MESSAGES:")
            normalMessages.take(8).forEach { msg ->
                stringBuilder.appendLine("  • ${msg.senderName}: ${msg.content.take(40)}")
                stringBuilder.appendLine("    📍 ${String.format("%.4f", msg.latitude)}, ${String.format("%.4f", msg.longitude)}")
                stringBuilder.appendLine("    🔋 ${msg.batteryLevel}% battery • ${formatTimestamp(msg.timestamp)}")
                stringBuilder.appendLine()
            }
        }

        binding.tvLocationOverview.text = stringBuilder.toString()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }

    private fun showEmergencyZones() {
        // Focus on emergency zones - could implement clustering/heatmap here
        viewModel.sosMessages.asLiveData().observe(viewLifecycleOwner) { sosMessages ->
            if (sosMessages.isNotEmpty()) {
                val zones = sosMessages.groupBy { 
                    "${String.format("%.2f", it.latitude)},${String.format("%.2f", it.longitude)}"
                }
                val highPriorityZones = zones.filter { it.value.size > 1 }
                
                val message = if (highPriorityZones.isNotEmpty()) {
                    "⚠️ ${highPriorityZones.size} high-priority emergency zone(s) with multiple alerts detected!"
                } else {
                    "📍 ${zones.size} emergency location(s) identified"
                }
                
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
