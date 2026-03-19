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
import androidx.recyclerview.widget.LinearLayoutManager
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
        sosListAdapter = SosListAdapter()
        binding.rvSosList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSosList.adapter = sosListAdapter

        viewModel.allMessages.asLiveData().observe(viewLifecycleOwner) { all ->
            val withLocation = all.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            val sosList = withLocation.filter { it.type == MessageType.SOS }
            val normalList = withLocation.filter { it.type == MessageType.NORMAL }

            sosListAdapter.submitList(sosList)
            binding.tvMapInfo.text =
                "SOS pins: ${sosList.size}  |  Messages with location: ${normalList.size}"

            // Draw simple ASCII grid overview (no-internet fallback)
            drawSimpleGrid(sosList + normalList)
        }
    }

    private fun drawSimpleGrid(messages: List<Message>) {
        if (messages.isEmpty()) {
            binding.tvGridMap.text = "No location data yet.\nMessages with GPS enabled will appear here."
            return
        }
        val sb = StringBuilder()
        sb.appendLine("=== Offline Location Overview ===\n")
        messages.take(10).forEachIndexed { i, msg ->
            val icon = if (msg.type == MessageType.SOS) "🚨" else "📍"
            sb.appendLine("$icon ${msg.senderName}: ${String.format("%.4f", msg.latitude)}, ${String.format("%.4f", msg.longitude)}")
            sb.appendLine("   ${msg.content.take(50)}  [${msg.batteryLevel}% battery]")
            sb.appendLine()
        }
        binding.tvGridMap.text = sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
