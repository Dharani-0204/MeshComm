package com.meshcomm.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.db.UserProfileEntity
import com.meshcomm.data.model.EmergencyContact
import com.meshcomm.data.model.UserProfile
import com.meshcomm.data.model.UserRole
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val profileDao = db.userProfileDao()
    private val gson = Gson()

    // ─── Profile CRUD Operations ────────────────────────────────────────────────

    suspend fun saveProfile(profile: UserProfile) {
        val entity = profile.toEntity()
        profileDao.insertOrUpdate(entity)
    }

    suspend fun getProfile(userId: String): UserProfile? {
        return profileDao.getProfile(userId)?.toDomain()
    }

    fun getProfileFlow(userId: String): Flow<UserProfile?> {
        return profileDao.getProfileFlow(userId).map { it?.toDomain() }
    }

    suspend fun updateProfileCompletion(userId: String, complete: Boolean) {
        profileDao.updateProfileCompletion(userId, complete)
    }

    suspend fun markSyncedWithFirebase(userId: String) {
        profileDao.updateSyncStatus(userId, true)
    }

    suspend fun getUnsyncedProfiles(): List<UserProfile> {
        return profileDao.getUnsyncedProfiles().map { it.toDomain() }
    }

    // ─── Entity to Domain Conversion ────────────────────────────────────────────

    private fun UserProfileEntity.toDomain(): UserProfile {
        val contactsType = object : TypeToken<List<EmergencyContact>>() {}.type
        val conditionsType = object : TypeToken<List<String>>() {}.type
        val allergiesType = object : TypeToken<List<String>>() {}.type

        return UserProfile(
            userId = odiumId,
            name = name,
            role = UserRole.valueOf(role),
            bloodGroup = bloodGroup,
            medicalConditions = if (medicalConditions.isBlank()) emptyList()
                else gson.fromJson(medicalConditions, conditionsType),
            allergies = if (allergies.isBlank()) emptyList()
                else gson.fromJson(allergies, allergiesType),
            emergencyContacts = if (emergencyContacts.isBlank()) emptyList()
                else gson.fromJson(emergencyContacts, contactsType),
            isProfileComplete = isProfileComplete,
            lastUpdated = lastUpdated
        )
    }

    private fun UserProfile.toEntity(): UserProfileEntity {
        return UserProfileEntity(
            odiumId = userId,
            name = name,
            role = role.name,
            bloodGroup = bloodGroup,
            medicalConditions = gson.toJson(medicalConditions),
            allergies = gson.toJson(allergies),
            emergencyContacts = gson.toJson(emergencyContacts),
            isProfileComplete = isProfileComplete,
            lastUpdated = lastUpdated,
            syncedWithFirebase = false
        )
    }

    // ─── Quick Profile Creation from Prefs ──────────────────────────────────────

    fun createProfileFromPrefs(context: Context): UserProfile {
        val userId = PrefsHelper.getUserId(context)
        val name = PrefsHelper.getUserName(context)
        val role = PrefsHelper.getUserRole(context)
        val bloodGroup = PrefsHelper.getBloodGroup(context)
        val conditions = PrefsHelper.getMedicalConditions(context)
        val allergies = PrefsHelper.getAllergies(context)
        val contacts = com.meshcomm.utils.EmergencyContactsManager.getContacts(context)
            .map { EmergencyContact(it.name, it.phone) }

        return UserProfile(
            userId = userId,
            name = name,
            role = role,
            bloodGroup = bloodGroup,
            medicalConditions = conditions,
            allergies = allergies,
            emergencyContacts = contacts,
            isProfileComplete = PrefsHelper.isProfileComplete(context)
        )
    }

    suspend fun syncProfileFromPrefs(context: Context) {
        val profile = createProfileFromPrefs(context)
        saveProfile(profile)
    }
}
