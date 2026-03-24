package com.meshcomm.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.meshcomm.R
import com.meshcomm.databinding.FragmentProfileBinding
import com.meshcomm.ui.contacts.ContactPickerDialog
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.EmergencyContactsManager
import com.meshcomm.utils.PrefsHelper

class ProfileFragment : Fragment() {

    companion object {
        private const val TAG = "ProfileFragment"
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()

        // User identity
        binding.tvUserId.text = "ID: ${PrefsHelper.getUserId(ctx)}"
        binding.tvUserName.text = PrefsHelper.getUserName(ctx)
        binding.tvUserRole.text = "Role: ${PrefsHelper.getUserRole(ctx).name}"

        // Battery chip
        val batteryLevel = BatteryHelper.getLevel(ctx)
        binding.chipBattery.text = "$batteryLevel%"

        // Relay chip
        val canRelay = BatteryHelper.canRelay(ctx)
        binding.chipRelay.text = if (canRelay) "Relay: ON" else "Relay: OFF"

        // Mesh status
        binding.tvActiveNodes.text = "0"
        binding.tvDeviceRole.text = if (canRelay) "Relay Node" else "Client"

        // Name edit
        binding.etNameEdit.setText(PrefsHelper.getUserName(ctx))
        binding.btnSaveName.setOnClickListener {
            val name = binding.etNameEdit.text.toString().trim()
            if (name.isNotEmpty()) {
                PrefsHelper.setUserName(ctx, name)
                binding.tvUserName.text = name
                Toast.makeText(ctx, "Name updated", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "User name updated: $name")
            }
        }

        // Load and display emergency contacts
        refreshEmergencyContactsUI()

        // Contact picker button
        binding.btnPickContacts.setOnClickListener {
            Log.d(TAG, "Opening contact picker dialog")
            val dialog = ContactPickerDialog.newInstance()
            dialog.setOnContactsSelectedListener { selectedContacts ->
                Log.i(TAG, "Selected ${selectedContacts.size} emergency contacts")
                refreshEmergencyContactsUI()
            }
            dialog.show(parentFragmentManager, "contact_picker")
        }

        // Logout logic
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? This will stop the mesh service and clear your session, but your messages will be kept.")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        val ctx = requireContext()
        
        // 1. Stop MeshService
        com.meshcomm.mesh.MeshService.stop(ctx)
        
        // 2. Clear session from PrefsHelper
        PrefsHelper.clearSession(ctx)
        
        // 3. Navigate to SetupActivity with cleared backstack
        try {
            val intent = android.content.Intent(ctx, Class.forName("com.meshcomm.ui.setup.SetupActivity")).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout navigation", e)
        }
    }

    private fun refreshEmergencyContactsUI() {
        val contacts = EmergencyContactsManager.getContacts(requireContext())
        val count = contacts.size

        // Update count chip
        binding.chipContactCount.text = count.toString()

        if (count > 0) {
            // Show contacts list
            binding.tvNoContacts.visibility = View.GONE
            binding.contactsListContainer.visibility = View.VISIBLE
            binding.contactsListContainer.removeAllViews()

            contacts.forEach { contact ->
                val contactView = createContactItemView(contact.name, contact.phone)
                binding.contactsListContainer.addView(contactView)
            }

            Log.d(TAG, "Displaying $count emergency contacts")
        } else {
            // Show empty state
            binding.tvNoContacts.visibility = View.VISIBLE
            binding.contactsListContainer.visibility = View.GONE
            Log.d(TAG, "No emergency contacts to display")
        }
    }

    private fun createContactItemView(name: String, phone: String): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val bulletView = TextView(requireContext()).apply {
            text = "•"
            textSize = 16f
            setTextColor(resources.getColor(R.color.sos_red, null))
            setPadding(0, 0, 12, 0)
        }

        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameView = TextView(requireContext()).apply {
            text = name
            textSize = 14f
            setTextColor(resources.getColor(R.color.md_theme_onSurface, null))
        }

        val phoneView = TextView(requireContext()).apply {
            text = phone
            textSize = 12f
            setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
        }

        infoLayout.addView(nameView)
        infoLayout.addView(phoneView)

        layout.addView(bulletView)
        layout.addView(infoLayout)

        return layout
    }

    override fun onResume() {
        super.onResume()
        refreshEmergencyContactsUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
