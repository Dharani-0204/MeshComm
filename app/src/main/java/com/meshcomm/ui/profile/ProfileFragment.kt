package com.meshcomm.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.meshcomm.R
import com.meshcomm.databinding.FragmentProfileBinding
import com.meshcomm.ui.contacts.ContactPickerDialog
import com.meshcomm.ui.dashboard.RescueDashboardActivity
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.EmergencyContactsManager
import com.meshcomm.utils.PrefsHelper

class ProfileFragment : Fragment() {

    companion object {
        private const val TAG = "ProfileFragment"
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()

        setupIdentityCard()
        setupRoleBasedUI()
        setupBloodGroupDropdown()
        setupEditModeObserver()
        setupProfileObserver()
        setupButtonListeners()
        refreshEmergencyContactsUI()
    }

    private fun setupIdentityCard() {
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

        // Profile status chip
        updateProfileStatusChip()

        // Mesh status
        binding.tvActiveNodes.text = "0"
        binding.tvDeviceRole.text = if (canRelay) "Relay Node" else "Client"
    }

    private fun setupRoleBasedUI() {
        val isCivilian = viewModel.isCivilian()
        val isRescuer = viewModel.isRescuer()

        // Medical profile card - ONLY for civilians
        binding.cardMedicalProfile.visibility = if (isCivilian) View.VISIBLE else View.GONE

        // Profile status chip - ONLY for civilians
        binding.chipProfileStatus.visibility = if (isCivilian) View.VISIBLE else View.GONE

        // Rescuer dashboard card - ONLY for rescuers
        binding.cardRescuerDashboard.visibility = if (isRescuer) View.VISIBLE else View.GONE

        // Profile reminder - ONLY for civilians with incomplete profile
        if (viewModel.showProfileReminder.value == true) {
            binding.cardProfileReminder.visibility = View.VISIBLE
        }

        Log.d(TAG, "Role-based UI setup: isCivilian=$isCivilian, isRescuer=$isRescuer")
    }

    private fun setupBloodGroupDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            viewModel.bloodGroups
        )
        binding.actvBloodGroup.setAdapter(adapter)
    }

    private fun setupEditModeObserver() {
        viewModel.isEditMode.observe(viewLifecycleOwner) { isEditMode ->
            updateEditModeUI(isEditMode)
        }
    }

    private fun setupProfileObserver() {
        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe

            // Update name
            binding.tvUserName.text = profile.name
            binding.etNameEdit.setText(profile.name)

            // Update medical info (civilians only)
            if (viewModel.isCivilian()) {
                binding.tvBloodGroup.text = profile.bloodGroup.ifEmpty { "Not specified" }
                binding.actvBloodGroup.setText(profile.bloodGroup, false)

                binding.tvMedicalConditions.text = if (profile.medicalConditions.isEmpty()) {
                    "None specified"
                } else {
                    profile.medicalConditions.joinToString(", ")
                }
                binding.etMedicalConditions.setText(profile.medicalConditions.joinToString(", "))

                binding.tvAllergies.text = if (profile.allergies.isEmpty()) {
                    "None specified"
                } else {
                    profile.allergies.joinToString(", ")
                }
                binding.etAllergies.setText(profile.allergies.joinToString(", "))

                updateProfileStatusChip()
            }

            Log.d(TAG, "Profile updated: ${profile.name}, complete=${profile.isProfileComplete}")
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            success ?: return@observe
            if (success) {
                Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearSaveSuccess()
        }

        viewModel.showProfileReminder.observe(viewLifecycleOwner) { show ->
            binding.cardProfileReminder.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun updateEditModeUI(isEditMode: Boolean) {
        // Toggle button text
        binding.btnToggleEdit.text = if (isEditMode) "Cancel" else "Edit"

        // Name field
        binding.tvUserName.visibility = if (isEditMode) View.GONE else View.VISIBLE
        binding.tilName.visibility = if (isEditMode) View.VISIBLE else View.GONE

        // Medical fields (civilians only)
        if (viewModel.isCivilian()) {
            binding.tvBloodGroup.visibility = if (isEditMode) View.GONE else View.VISIBLE
            binding.tilBloodGroup.visibility = if (isEditMode) View.VISIBLE else View.GONE

            binding.tvMedicalConditions.visibility = if (isEditMode) View.GONE else View.VISIBLE
            binding.tilMedicalConditions.visibility = if (isEditMode) View.VISIBLE else View.GONE

            binding.tvAllergies.visibility = if (isEditMode) View.GONE else View.VISIBLE
            binding.tilAllergies.visibility = if (isEditMode) View.VISIBLE else View.GONE
        }

        // Save button
        binding.btnSaveProfile.visibility = if (isEditMode) View.VISIBLE else View.GONE

        Log.d(TAG, "Edit mode: $isEditMode")
    }

    private fun updateProfileStatusChip() {
        val isComplete = PrefsHelper.isProfileComplete(requireContext())
        if (viewModel.isCivilian()) {
            binding.chipProfileStatus.text = if (isComplete) "Profile: Complete" else "Profile: Incomplete"
            binding.chipProfileStatus.setChipBackgroundColorResource(
                if (isComplete) R.color.mesh_active else R.color.sos_red
            )
        }
    }

    private fun setupButtonListeners() {
        // Edit toggle
        binding.btnToggleEdit.setOnClickListener {
            viewModel.toggleEditMode()
        }

        // Save profile
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        // Contact picker
        binding.btnPickContacts.setOnClickListener {
            Log.d(TAG, "Opening contact picker dialog")
            val dialog = ContactPickerDialog.newInstance()
            dialog.setOnContactsSelectedListener { selectedContacts ->
                Log.i(TAG, "Selected ${selectedContacts.size} emergency contacts")
                refreshEmergencyContactsUI()
            }
            dialog.show(parentFragmentManager, "contact_picker")
        }

        // Profile reminder buttons
        binding.btnDismissReminder.setOnClickListener {
            viewModel.dismissProfileReminder()
        }

        binding.btnCompleteProfile.setOnClickListener {
            viewModel.dismissProfileReminder()
            viewModel.setEditMode(true)
        }

        // Open dashboard (rescuers only)
        binding.btnOpenDashboard.setOnClickListener {
            val intent = Intent(requireContext(), RescueDashboardActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveProfile() {
        val name = binding.etNameEdit.text.toString().trim()
        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }
        binding.tilName.error = null

        val bloodGroup = binding.actvBloodGroup.text.toString()
        val conditions = binding.etMedicalConditions.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val allergies = binding.etAllergies.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        viewModel.saveProfile(name, bloodGroup, conditions, allergies)
        Log.d(TAG, "Saving profile: name=$name, bloodGroup=$bloodGroup")
    }

    private fun refreshEmergencyContactsUI() {
        val contacts = EmergencyContactsManager.getContacts(requireContext())
        val count = contacts.size

        // Update count chip
        binding.chipContactCount.text = count.toString()

        if (count > 0) {
            binding.tvNoContacts.visibility = View.GONE
            binding.contactsListContainer.visibility = View.VISIBLE
            binding.contactsListContainer.removeAllViews()

            contacts.forEach { contact ->
                val contactView = createContactItemView(contact.name, contact.phone)
                binding.contactsListContainer.addView(contactView)
            }

            Log.d(TAG, "Displaying $count emergency contacts")
        } else {
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
        viewModel.loadProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
