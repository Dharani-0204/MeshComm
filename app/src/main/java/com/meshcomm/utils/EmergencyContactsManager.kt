package com.meshcomm.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Emergency contact data class
 */
data class SavedEmergencyContact(
    val name: String,
    val phone: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Manager for storing and retrieving emergency contacts
 * Uses SharedPreferences with JSON serialization
 */
object EmergencyContactsManager {
    private const val TAG = "EmergencyContactsMgr"
    private const val PREFS_NAME = "emergency_contacts_prefs"
    private const val KEY_CONTACTS = "saved_contacts"

    private val gson = Gson()

    /**
     * Save list of emergency contacts
     */
    fun saveContacts(context: Context, contacts: List<SavedEmergencyContact>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(contacts)
        prefs.edit().putString(KEY_CONTACTS, json).apply()
        Log.d(TAG, "Saved ${contacts.size} emergency contacts")
    }

    /**
     * Get all saved emergency contacts
     */
    fun getContacts(context: Context): List<SavedEmergencyContact> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<SavedEmergencyContact>>() {}.type
            val contacts: List<SavedEmergencyContact> = gson.fromJson(json, type)
            Log.d(TAG, "Loaded ${contacts.size} emergency contacts")
            contacts
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load contacts: ${e.message}")
            emptyList()
        }
    }

    /**
     * Add a single contact
     */
    fun addContact(context: Context, contact: SavedEmergencyContact) {
        val contacts = getContacts(context).toMutableList()
        // Avoid duplicates by phone number
        if (contacts.none { it.phone == contact.phone }) {
            contacts.add(contact)
            saveContacts(context, contacts)
            Log.d(TAG, "Added emergency contact: ${contact.name} (${contact.phone})")
        } else {
            Log.d(TAG, "Contact already exists: ${contact.phone}")
        }
    }

    /**
     * Remove a contact by phone number
     */
    fun removeContact(context: Context, phone: String) {
        val contacts = getContacts(context).toMutableList()
        val removed = contacts.removeAll { it.phone == phone }
        if (removed) {
            saveContacts(context, contacts)
            Log.d(TAG, "Removed emergency contact: $phone")
        }
    }

    /**
     * Clear all emergency contacts
     */
    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CONTACTS).apply()
        Log.d(TAG, "Cleared all emergency contacts")
    }

    /**
     * Get phone numbers only (for SMS sending)
     */
    fun getPhoneNumbers(context: Context): List<String> {
        return getContacts(context).map { it.phone }
    }

    /**
     * Check if any emergency contacts are saved
     */
    fun hasContacts(context: Context): Boolean {
        return getContacts(context).isNotEmpty()
    }

    /**
     * Get contact count
     */
    fun getContactCount(context: Context): Int {
        return getContacts(context).size
    }
}
