package com.meshcomm.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.meshcomm.data.model.EmergencyContact
import com.meshcomm.data.model.UserProfile
import com.meshcomm.data.model.UserRole
import com.meshcomm.data.repository.ProfileRepository
import com.meshcomm.utils.EmergencyContactsManager
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val context get() = getApplication<Application>()

    // ─── UI State ───────────────────────────────────────────────────────────────

    private val _profile = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveSuccess = MutableLiveData<Boolean?>()
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    private val _showProfileReminder = MutableLiveData(false)
    val showProfileReminder: LiveData<Boolean> = _showProfileReminder

    // Blood group options
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")

    // ─── Initialization ─────────────────────────────────────────────────────────

    init {
        loadProfile()
        checkProfileReminder()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val userId = PrefsHelper.getUserId(context)
            val existing = repository.getProfile(userId)

            if (existing != null) {
                _profile.value = existing
            } else {
                // Create profile from prefs if not in DB
                val profile = repository.createProfileFromPrefs(context)
                repository.saveProfile(profile)
                _profile.value = profile
            }
        }
    }

    private fun checkProfileReminder() {
        if (PrefsHelper.shouldShowProfileReminder(context)) {
            _showProfileReminder.value = true
        }
    }

    // ─── Edit Mode ──────────────────────────────────────────────────────────────

    fun toggleEditMode() {
        _isEditMode.value = !(_isEditMode.value ?: false)
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    // ─── Role Checks ────────────────────────────────────────────────────────────

    fun isCivilian(): Boolean = PrefsHelper.isCivilian(context)

    fun isRescuer(): Boolean = PrefsHelper.isRescuer(context)

    fun getUserRole(): UserRole = PrefsHelper.getUserRole(context)

    // ─── Save Profile ───────────────────────────────────────────────────────────

    fun saveProfile(
        name: String,
        bloodGroup: String,
        medicalConditions: List<String>,
        allergies: List<String>
    ) {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                // Update prefs
                PrefsHelper.setUserName(context, name)
                PrefsHelper.setBloodGroup(context, bloodGroup)
                PrefsHelper.setMedicalConditions(context, medicalConditions.filter { it.isNotBlank() })
                PrefsHelper.setAllergies(context, allergies.filter { it.isNotBlank() })

                // Check if profile is complete (for civilians)
                val isComplete = if (isCivilian()) {
                    name.isNotBlank() && bloodGroup.isNotBlank() && bloodGroup != "Unknown"
                } else {
                    true // Rescuers are always "complete"
                }
                PrefsHelper.setProfileComplete(context, isComplete)

                // Get emergency contacts
                val contacts = EmergencyContactsManager.getContacts(context)
                    .map { EmergencyContact(it.name, it.phone) }

                // Create updated profile
                val updatedProfile = UserProfile(
                    userId = PrefsHelper.getUserId(context),
                    name = name,
                    role = PrefsHelper.getUserRole(context),
                    bloodGroup = bloodGroup,
                    medicalConditions = medicalConditions.filter { it.isNotBlank() },
                    allergies = allergies.filter { it.isNotBlank() },
                    emergencyContacts = contacts,
                    isProfileComplete = isComplete,
                    lastUpdated = System.currentTimeMillis()
                )

                // Save to DB
                repository.saveProfile(updatedProfile)

                _profile.value = updatedProfile
                _isEditMode.value = false
                _saveSuccess.value = true

            } catch (e: Exception) {
                _saveSuccess.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ─── Quick Save Name Only ───────────────────────────────────────────────────

    fun saveName(name: String) {
        viewModelScope.launch {
            PrefsHelper.setUserName(context, name)
            repository.syncProfileFromPrefs(context)
            loadProfile()
        }
    }

    // ─── Reminder Handling ──────────────────────────────────────────────────────

    fun dismissProfileReminder() {
        _showProfileReminder.value = false
        PrefsHelper.incrementProfileReminderCount(context)
        PrefsHelper.setLastProfileReminderTime(context)
    }

    fun skipProfile() {
        PrefsHelper.setProfileSkipped(context, true)
        _showProfileReminder.value = false
    }

    // ─── Clear Save Success State ───────────────────────────────────────────────

    fun clearSaveSuccess() {
        _saveSuccess.value = null
    }
}
