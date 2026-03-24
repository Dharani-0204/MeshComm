package com.meshcomm.utils

import android.content.Context
import android.content.SharedPreferences
import com.meshcomm.data.model.UserRole
import java.util.UUID

object PrefsHelper {
    private const val PREFS_NAME = "meshcomm_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_SETUP_DONE = "setup_done"
    private const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"

    // Profile-related keys
    private const val KEY_PROFILE_COMPLETE = "profile_complete"
    private const val KEY_PROFILE_SKIPPED = "profile_skipped"
    private const val KEY_PROFILE_REMINDER_COUNT = "profile_reminder_count"
    private const val KEY_LAST_PROFILE_REMINDER = "last_profile_reminder"
    private const val KEY_BLOOD_GROUP = "blood_group"
    private const val KEY_MEDICAL_CONDITIONS = "medical_conditions"
    private const val KEY_ALLERGIES = "allergies"
    private const val KEY_LAST_LOCATION_NAME = "last_location_name"
    private const val KEY_FIREBASE_SYNCED = "firebase_synced"

    // Network health keys
    private const val KEY_NETWORK_PEER_COUNT = "network_peer_count"
    private const val KEY_NETWORK_HEALTHY = "network_healthy"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUserId(ctx: Context): String {
        var id = prefs(ctx).getString(KEY_USER_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString().take(8)
            prefs(ctx).edit().putString(KEY_USER_ID, id).apply()
        }
        return id
    }

    fun getUserName(ctx: Context): String =
        prefs(ctx).getString(KEY_USER_NAME, "Unknown") ?: "Unknown"

    fun setUserName(ctx: Context, name: String) =
        prefs(ctx).edit().putString(KEY_USER_NAME, name).apply()

    fun getUserRole(ctx: Context): UserRole {
        val role = prefs(ctx).getString(KEY_USER_ROLE, UserRole.CIVILIAN.name)
        return UserRole.valueOf(role ?: UserRole.CIVILIAN.name)
    }

    fun setUserRole(ctx: Context, role: UserRole) =
        prefs(ctx).edit().putString(KEY_USER_ROLE, role.name).apply()

    fun isSetupDone(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_SETUP_DONE, false)

    fun setSetupDone(ctx: Context) = prefs(ctx).edit().putBoolean(KEY_SETUP_DONE, true).apply()

    fun getEmergencyContacts(ctx: Context): List<String> {
        val str = prefs(ctx).getString(KEY_EMERGENCY_CONTACTS, "") ?: ""
        return if (str.isBlank()) emptyList() else str.split(",")
    }

    fun setEmergencyContacts(ctx: Context, contacts: List<String>) {
        prefs(ctx).edit().putString(KEY_EMERGENCY_CONTACTS, contacts.joinToString(",")).apply()
    }

    // ─── Profile Management ─────────────────────────────────────────────────────

    fun isProfileComplete(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_PROFILE_COMPLETE, false)

    fun setProfileComplete(ctx: Context, complete: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_PROFILE_COMPLETE, complete).apply()

    fun wasProfileSkipped(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_PROFILE_SKIPPED, false)

    fun setProfileSkipped(ctx: Context, skipped: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_PROFILE_SKIPPED, skipped).apply()

    fun getProfileReminderCount(ctx: Context): Int =
        prefs(ctx).getInt(KEY_PROFILE_REMINDER_COUNT, 0)

    fun incrementProfileReminderCount(ctx: Context) {
        val count = getProfileReminderCount(ctx) + 1
        prefs(ctx).edit().putInt(KEY_PROFILE_REMINDER_COUNT, count).apply()
    }

    fun getLastProfileReminderTime(ctx: Context): Long =
        prefs(ctx).getLong(KEY_LAST_PROFILE_REMINDER, 0)

    fun setLastProfileReminderTime(ctx: Context, time: Long = System.currentTimeMillis()) =
        prefs(ctx).edit().putLong(KEY_LAST_PROFILE_REMINDER, time).apply()

    fun shouldShowProfileReminder(ctx: Context): Boolean {
        // Don't show for rescuers or if profile is complete
        if (getUserRole(ctx) != UserRole.CIVILIAN) return false
        if (isProfileComplete(ctx)) return false

        // Show reminder once every 24 hours, max 5 times
        val lastReminder = getLastProfileReminderTime(ctx)
        val reminderCount = getProfileReminderCount(ctx)
        val hoursSinceLastReminder = (System.currentTimeMillis() - lastReminder) / (1000 * 60 * 60)

        return reminderCount < 5 && hoursSinceLastReminder >= 24
    }

    // ─── Medical Profile Fields ─────────────────────────────────────────────────

    fun getBloodGroup(ctx: Context): String =
        prefs(ctx).getString(KEY_BLOOD_GROUP, "") ?: ""

    fun setBloodGroup(ctx: Context, bloodGroup: String) =
        prefs(ctx).edit().putString(KEY_BLOOD_GROUP, bloodGroup).apply()

    fun getMedicalConditions(ctx: Context): List<String> {
        val str = prefs(ctx).getString(KEY_MEDICAL_CONDITIONS, "") ?: ""
        return if (str.isBlank()) emptyList() else str.split("|||")
    }

    fun setMedicalConditions(ctx: Context, conditions: List<String>) =
        prefs(ctx).edit().putString(KEY_MEDICAL_CONDITIONS, conditions.joinToString("|||")).apply()

    fun getAllergies(ctx: Context): List<String> {
        val str = prefs(ctx).getString(KEY_ALLERGIES, "") ?: ""
        return if (str.isBlank()) emptyList() else str.split("|||")
    }

    fun setAllergies(ctx: Context, allergies: List<String>) =
        prefs(ctx).edit().putString(KEY_ALLERGIES, allergies.joinToString("|||")).apply()

    fun getLastLocationName(ctx: Context): String =
        prefs(ctx).getString(KEY_LAST_LOCATION_NAME, "") ?: ""

    fun setLastLocationName(ctx: Context, locationName: String) =
        prefs(ctx).edit().putString(KEY_LAST_LOCATION_NAME, locationName).apply()

    // ─── Firebase Sync ──────────────────────────────────────────────────────────

    fun isFirebaseSynced(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_FIREBASE_SYNCED, false)

    fun setFirebaseSynced(ctx: Context, synced: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_FIREBASE_SYNCED, synced).apply()

    // ─── Role-Based Helpers ─────────────────────────────────────────────────────

    fun isCivilian(ctx: Context): Boolean = getUserRole(ctx) == UserRole.CIVILIAN

    fun isRescuer(ctx: Context): Boolean =
        getUserRole(ctx) == UserRole.RESCUER || getUserRole(ctx) == UserRole.AUTHORITY

    fun needsEmergencyProfile(ctx: Context): Boolean =
        isCivilian(ctx) && !isProfileComplete(ctx)

    // ─── Network Health Tracking ────────────────────────────────────────────────

    fun getNetworkPeerCount(ctx: Context): Int =
        prefs(ctx).getInt(KEY_NETWORK_PEER_COUNT, 0)

    fun setNetworkPeerCount(ctx: Context, count: Int) =
        prefs(ctx).edit().putInt(KEY_NETWORK_PEER_COUNT, count).apply()

    fun isNetworkHealthy(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NETWORK_HEALTHY, false)

    fun setNetworkHealthy(ctx: Context, healthy: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_NETWORK_HEALTHY, healthy).apply()
}
