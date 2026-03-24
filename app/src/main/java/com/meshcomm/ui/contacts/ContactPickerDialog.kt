package com.meshcomm.ui.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.meshcomm.R
import com.meshcomm.utils.EmergencyContactsManager
import com.meshcomm.utils.SavedEmergencyContact

/**
 * Phone contact for display
 */
data class PhoneContact(
    val name: String,
    val phone: String,
    var isSelected: Boolean = false
)

/**
 * Dialog for picking emergency contacts from phone
 */
class ContactPickerDialog : DialogFragment() {

    companion object {
        private const val TAG = "ContactPickerDialog"
        const val REQUEST_READ_CONTACTS = 102

        fun newInstance(): ContactPickerDialog {
            return ContactPickerDialog()
        }
    }

    private var allContacts = mutableListOf<PhoneContact>()
    private var adapter: ContactAdapter? = null
    private var onContactsSelected: ((List<PhoneContact>) -> Unit)? = null

    fun setOnContactsSelectedListener(listener: (List<PhoneContact>) -> Unit) {
        onContactsSelected = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_contact_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchEdit = view.findViewById<TextInputEditText>(R.id.etSearch)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvContacts)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val tvSelectedCount = view.findViewById<TextView>(R.id.tvSelectedCount)
        val emptyState = view.findViewById<TextView>(R.id.tvEmptyState)

        // Setup RecyclerView
        adapter = ContactAdapter { contact ->
            contact.isSelected = !contact.isSelected
            adapter?.notifyDataSetChanged()
            updateSelectedCount(tvSelectedCount)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Load contacts
        if (hasContactsPermission()) {
            loadContacts()
            if (allContacts.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                emptyState.text = "No contacts found on this device"
            } else {
                emptyState.visibility = View.GONE
                adapter?.submitList(allContacts)
            }
        } else {
            emptyState.visibility = View.VISIBLE
            emptyState.text = "Contacts permission required"
            requestContactsPermission()
        }

        // Mark already saved contacts as selected
        val savedContacts = EmergencyContactsManager.getContacts(requireContext())
        val savedPhones = savedContacts.map { it.phone.replace(Regex("[^+0-9]"), "") }
        allContacts.forEach { contact ->
            val cleanPhone = contact.phone.replace(Regex("[^+0-9]"), "")
            contact.isSelected = savedPhones.any { it == cleanPhone }
        }
        adapter?.notifyDataSetChanged()
        updateSelectedCount(tvSelectedCount)

        // Search functionality
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter?.filter?.filter(s?.toString() ?: "")
            }
        })

        // Cancel button
        btnCancel.setOnClickListener {
            dismiss()
        }

        // Save button
        btnSave.setOnClickListener {
            val selectedContacts = allContacts.filter { it.isSelected }
            if (selectedContacts.isEmpty()) {
                Toast.makeText(requireContext(), "No contacts selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save to EmergencyContactsManager
            val savedList = selectedContacts.map {
                SavedEmergencyContact(it.name, it.phone)
            }
            EmergencyContactsManager.saveContacts(requireContext(), savedList)

            Log.i(TAG, "Saved ${selectedContacts.size} emergency contacts")
            onContactsSelected?.invoke(selectedContacts)
            Toast.makeText(
                requireContext(),
                "${selectedContacts.size} emergency contacts saved",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }
    }

    private fun updateSelectedCount(tvCount: TextView) {
        val count = allContacts.count { it.isSelected }
        tvCount.text = "$count selected"
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.READ_CONTACTS),
            REQUEST_READ_CONTACTS
        )
    }

    private fun loadContacts() {
        Log.d(TAG, "Loading phone contacts")
        allContacts.clear()

        try {
            val cursor = requireContext().contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenPhones = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: continue
                    val phone = it.getString(phoneIndex) ?: continue
                    val cleanPhone = phone.replace(Regex("[^+0-9]"), "")

                    // Avoid duplicates
                    if (cleanPhone !in seenPhones && cleanPhone.length >= 10) {
                        seenPhones.add(cleanPhone)
                        allContacts.add(PhoneContact(name, phone))
                    }
                }
            }

            Log.d(TAG, "Loaded ${allContacts.size} unique contacts")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading contacts: ${e.message}", e)
        }
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 90% of screen
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    /**
     * Adapter for contacts RecyclerView with filtering
     */
    inner class ContactAdapter(
        private val onItemClick: (PhoneContact) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ViewHolder>(), Filterable {

        private var displayList = mutableListOf<PhoneContact>()

        fun submitList(list: List<PhoneContact>) {
            displayList = list.toMutableList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contact, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = displayList[position]
            holder.bind(contact)
        }

        override fun getItemCount() = displayList.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tvContactName)
            private val tvPhone: TextView = view.findViewById(R.id.tvContactPhone)
            private val checkbox: CheckBox = view.findViewById(R.id.cbContact)

            fun bind(contact: PhoneContact) {
                tvName.text = contact.name
                tvPhone.text = contact.phone
                checkbox.isChecked = contact.isSelected

                itemView.setOnClickListener { onItemClick(contact) }
                checkbox.setOnClickListener { onItemClick(contact) }
            }
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.lowercase() ?: ""
                    val filtered = if (query.isEmpty()) {
                        allContacts
                    } else {
                        allContacts.filter {
                            it.name.lowercase().contains(query) ||
                                    it.phone.contains(query)
                        }
                    }
                    return FilterResults().apply { values = filtered }
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    displayList = (results?.values as? List<PhoneContact>)?.toMutableList()
                        ?: mutableListOf()
                    notifyDataSetChanged()
                }
            }
        }
    }
}
