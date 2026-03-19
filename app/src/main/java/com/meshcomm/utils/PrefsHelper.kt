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
}
