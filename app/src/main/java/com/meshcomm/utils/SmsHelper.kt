package com.meshcomm.utils

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility for sending SMS emergency alerts
 * Handles permissions, delivery reports, and multi-part messages
 */
object SmsHelper {
    private const val TAG = "SmsHelper"
    const val REQUEST_SMS_PERMISSION = 201

    /**
     * Result of SMS send attempt
     */
    data class SmsResult(
        val phoneNumber: String,
        val success: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Callback for SMS send results
     */
    interface SmsCallback {
        fun onSmsSent(result: SmsResult)
        fun onAllSmsCompleted(results: List<SmsResult>)
    }

    /**
     * Check if SMS permission is granted
     */
    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request SMS permission from activity
     */
    fun requestSmsPermission(activity: Activity) {
        Log.d(TAG, "Requesting SMS permission")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE
            ),
            REQUEST_SMS_PERMISSION
        )
    }

    /**
     * Send SMS to a single phone number
     */
    fun sendSms(
        context: Context,
        phoneNumber: String,
        message: String,
        callback: ((SmsResult) -> Unit)? = null
    ) {
        if (!hasSmsPermission(context)) {
            Log.e(TAG, "SMS permission not granted")
            callback?.invoke(SmsResult(phoneNumber, false, "SMS permission not granted"))
            return
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Create sent intent for delivery confirmation
            val sentAction = "SMS_SENT_$phoneNumber"
            val sentIntent = PendingIntent.getBroadcast(
                context,
                phoneNumber.hashCode(),
                Intent(sentAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Register receiver for sent status
            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val success = resultCode == Activity.RESULT_OK
                    val errorMsg = when (resultCode) {
                        Activity.RESULT_OK -> null
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic failure"
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
                        SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
                        else -> "Unknown error ($resultCode)"
                    }

                    Log.d(TAG, "SMS to $phoneNumber: ${if (success) "SENT" else "FAILED - $errorMsg"}")
                    callback?.invoke(SmsResult(phoneNumber, success, errorMsg))

                    // Unregister receiver
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to unregister SMS receiver: ${e.message}")
                    }
                }
            }

            // Register the receiver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    sentReceiver,
                    IntentFilter(sentAction),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(sentReceiver, IntentFilter(sentAction))
            }

            // Check if message needs to be split (SMS limit is 160 chars)
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                val sentIntents = ArrayList<PendingIntent>()
                repeat(parts.size) { sentIntents.add(sentIntent) }
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    sentIntents,
                    null
                )
                Log.d(TAG, "Sending multi-part SMS (${parts.size} parts) to $phoneNumber")
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    sentIntent,
                    null
                )
                Log.d(TAG, "Sending SMS to $phoneNumber: ${message.take(50)}...")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber: ${e.message}", e)
            callback?.invoke(SmsResult(phoneNumber, false, e.message))
        }
    }

    /**
     * Send SMS to multiple phone numbers
     */
    fun sendSmsToMultiple(
        context: Context,
        phoneNumbers: List<String>,
        message: String,
        callback: SmsCallback? = null
    ) {
        if (phoneNumbers.isEmpty()) {
            Log.w(TAG, "No phone numbers provided for SMS")
            callback?.onAllSmsCompleted(emptyList())
            return
        }

        Log.i(TAG, "Sending SMS to ${phoneNumbers.size} contacts")
        val results = mutableListOf<SmsResult>()
        var completedCount = 0

        phoneNumbers.forEach { phone ->
            val cleanPhone = phone.replace(Regex("[^+0-9]"), "")
            if (cleanPhone.isNotEmpty()) {
                sendSms(context, cleanPhone, message) { result ->
                    results.add(result)
                    completedCount++
                    callback?.onSmsSent(result)

                    if (completedCount >= phoneNumbers.size) {
                        callback?.onAllSmsCompleted(results)
                        Log.i(TAG, "All SMS completed: ${results.count { it.success }}/${results.size} successful")
                    }
                }
            } else {
                Log.w(TAG, "Invalid phone number: $phone")
                completedCount++
                results.add(SmsResult(phone, false, "Invalid phone number"))
            }
        }
    }

    /**
     * Build emergency SOS message with location
     */
    fun buildSosMessage(
        senderName: String,
        latitude: Double,
        longitude: Double,
        address: String?,
        customMessage: String? = null
    ): String {
        val locationPart = if (address != null && address != "Unknown location") {
            "Location: $address"
        } else if (latitude != 0.0 || longitude != 0.0) {
            "Location: $latitude, $longitude"
        } else {
            "Location: Unknown"
        }

        val mapLink = if (latitude != 0.0 || longitude != 0.0) {
            "\nMap: https://maps.google.com/?q=$latitude,$longitude"
        } else ""

        val custom = if (!customMessage.isNullOrBlank()) "\n$customMessage" else ""

        return "SOS EMERGENCY from $senderName!\n$locationPart$mapLink$custom\n\nSent via ResQNet"
    }
}
