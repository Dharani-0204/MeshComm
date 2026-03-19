package com.meshcomm.ui.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.meshcomm.databinding.FragmentSosBinding
import com.meshcomm.ui.broadcast.MessageAdapter
import com.meshcomm.ui.home.MeshViewModel

class SOSFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MessageAdapter(requireContext())
        binding.rvSosAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSosAlerts.adapter = adapter

        viewModel.sosMessages.asLiveData().observe(viewLifecycleOwner) { msgs ->
            adapter.submitList(msgs)
            binding.tvSosCount.text = "${msgs.size} SOS alert(s) received"
        }

        binding.btnSOS.setOnClickListener {
            val custom = binding.etSosMessage.text.toString().trim()
            val msg = if (custom.isNotEmpty()) custom else "EMERGENCY! I need help!"
            viewModel.sendSOS(msg)
            Toast.makeText(requireContext(), "SOS SENT to mesh!", Toast.LENGTH_LONG).show()
        }

        binding.btnSOS.setOnLongClickListener {
            viewModel.sendSOS("CRITICAL EMERGENCY - Immediate assistance required!")
            Toast.makeText(requireContext(), "CRITICAL SOS SENT!", Toast.LENGTH_LONG).show()
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
