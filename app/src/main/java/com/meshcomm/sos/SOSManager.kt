package com.meshcomm.sos

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.model.SOSAlert
import com.meshcomm.data.repository.ProfileRepository
import com.meshcomm.data.repository.SOSAlertRepository
import com.meshcomm.location.LocationProvider
import com.meshcomm.mesh.MessageRouter
import com.meshcomm.mesh.PeerRegistry
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.GeocoderUtil
import com.meshcomm.utils.PrefsHelper
import com.meshcomm.utils.SmsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class SOSManager(
    private val context: Context,
    private val router: MessageRouter,
    private val locationProvider: LocationProvider
) {
    companion object {
        private const val TAG = "SOSManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val sosAlertRepository = SOSAlertRepository(context)
    private val profileRepository = ProfileRepository(context)

    fun sendSOS(customMessage: String = "EMERGENCY! I need help!") {
        val (lat, lon) = locationProvider.getLastLatLon()
        val connectedDevices = PeerRegistry.getConnectedCount()
        val deviceId = PrefsHelper.getUserId(context)
        val userName = PrefsHelper.getUserName(context)
        val timestamp = System.currentTimeMillis()
        val batteryLevel = BatteryHelper.getLevel(context)
        val alertId = UUID.randomUUID().toString()

        // Geocode location to address
        val address = if (lat != 0.0 && lon != 0.0) {
            GeocoderUtil.getShortAddress(context, lat, lon)
        } else {
            "Location unavailable"
        }

        // Format: "SOS|lat,lng|address|timestamp|deviceId|message"
        val formattedContent = "SOS|$lat,$lon|$address|$timestamp|$deviceId|$customMessage"

        Log.i(
            TAG, "Sending SOS: address='$address', location=($lat,$lon), " +
                    "connectedDevices=$connectedDevices, timestamp=$timestamp"
        )

        // Create mesh message
        val msg = Message(
            messageId = alertId,
            senderId = deviceId,
            senderName = userName,
            targetId = null,   // broadcast to all; auth/rescuers will display it
            type = MessageType.SOS,
            content = formattedContent,
            latitude = lat,
            longitude = lon,
            batteryLevel = batteryLevel,
            nearbyDevicesCount = connectedDevices,
            ttl = 10,           // extra hops for SOS
            deviceId = deviceId
        )
        router.sendMessage(msg)
        Log.d(TAG, "SOS sent to router: id=${msg.messageId}")

        // Create SOS alert for local storage and Firebase
        val sosAlert = SOSAlert(
            alertId = alertId,
            userId = deviceId,
            userName = userName,
            latitude = lat,
            longitude = lon,
            message = customMessage,
            timestamp = timestamp,
            batteryLevel = batteryLevel,
            isActive = true
        )

        // Save locally (offline-only)
        scope.launch {
            try {
                // Get profile snapshot for civilians
                val profileSnapshot = if (PrefsHelper.isCivilian(context)) {
                    profileRepository.getProfile(deviceId)
                } else null

                // Save to local database (offline)
                sosAlertRepository.saveAlert(sosAlert, profileSnapshot)
                Log.d(TAG, "SOS alert saved locally (offline): $alertId")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving SOS alert locally: ${e.message}", e)
            }
        }

        // Send SMS to emergency contacts
        sendSmsToEmergencyContacts(userName, lat, lon, address, customMessage)
    }

    private fun sendSmsToEmergencyContacts(
        userName: String,
        lat: Double,
        lon: Double,
        address: String,
        message: String
    ) {
        try {
            val contacts = com.meshcomm.utils.EmergencyContactsManager.getContacts(context)
            if (contacts.isEmpty()) {
                Log.d(TAG, "No emergency contacts to SMS")
                return
            }

            val smsMessage = buildString {
                appendLine("SOS ALERT from $userName!")
                appendLine()
                appendLine("Location: $address")
                appendLine("Coordinates: $lat, $lon")
                appendLine("Maps: https://maps.google.com/?q=$lat,$lon")
                appendLine()
                if (message.isNotBlank()) {
                    appendLine("Message: $message")
                }
                appendLine()
                appendLine("Sent via ResQNet Emergency App")
            }

            contacts.forEach { contact ->
                SmsHelper.sendSms(context, contact.phone, smsMessage)
                Log.d(TAG, "SMS sent to ${contact.name} (${contact.phone})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}", e)
        }
    }

    /** Play alert + vibrate on the RECEIVING device */
    fun triggerSOSAlert() {
        Log.d(TAG, "Triggering SOS alert (vibration + sound)")
        triggerLocalAlert()
    }

    private fun triggerLocalAlert() {
        // Vibration pattern: SOS  (... --- ...)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(
                0, 200, 100, 200, 100, 200, 300, 500, 100, 500, 100, 500, 300, 200, 100, 200, 100, 200
            )
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }

        // Ringtone
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ring = RingtoneManager.getRingtone(context, notification)
            ring.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ring.play()
            Log.d(TAG, "SOS alert sound playing")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing ringtone: ${e.message}")
        }
    }
}
