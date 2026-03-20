package com.meshcomm.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.meshcomm.databinding.FragmentProfileBinding
import com.meshcomm.data.model.UserRole
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        binding.tvUserId.text = "ID: ${PrefsHelper.getUserId(ctx)}"
        binding.tvUserName.text = PrefsHelper.getUserName(ctx)
        binding.tvUserRole.text = "Role: ${PrefsHelper.getUserRole(ctx).name}"
        
        // Update battery chip
        val batteryLevel = BatteryHelper.getLevel(ctx)
        binding.chipBattery.text = "🔋 $batteryLevel%"
        
        // Update relay chip
        val canRelay = BatteryHelper.canRelay(ctx)
        binding.chipRelay.text = if (canRelay) "📡 Relay: ON" else "📡 Relay: OFF"
        
        // Update mesh status
        binding.tvActiveNodes.text = "0"
        binding.tvDeviceRole.text = if (canRelay) "Relay Node" else "Client"

        binding.etNameEdit.setText(PrefsHelper.getUserName(ctx))

        binding.btnSaveName.setOnClickListener {
            val name = binding.etNameEdit.text.toString().trim()
            if (name.isNotEmpty()) {
                PrefsHelper.setUserName(ctx, name)
                binding.tvUserName.text = name
                Toast.makeText(ctx, "Name updated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etEmergencyContacts.setText(PrefsHelper.getEmergencyContacts(ctx).joinToString(", "))
        binding.btnSaveContacts.setOnClickListener {
            val contacts = binding.etEmergencyContacts.text.toString()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            PrefsHelper.setEmergencyContacts(ctx, contacts)
            Toast.makeText(ctx, "Emergency contacts saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
