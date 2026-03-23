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
import com.meshcomm.location.LocationProvider
import com.meshcomm.mesh.MessageRouter
import com.meshcomm.mesh.PeerRegistry
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.GeocoderUtil
import com.meshcomm.utils.PrefsHelper

class SOSManager(
    private val context: Context,
    private val router: MessageRouter,
    private val locationProvider: LocationProvider
) {
    companion object {
        private const val TAG = "SOSManager"
    }

    fun sendSOS(customMessage: String = "EMERGENCY! I need help!") {
        val (lat, lon) = locationProvider.getLastLatLon()
        val connectedDevices = PeerRegistry.getConnectedCount()
        val deviceId = PrefsHelper.getUserId(context)
        val timestamp = System.currentTimeMillis()

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

        val msg = Message(
            senderId = PrefsHelper.getUserId(context),
            senderName = PrefsHelper.getUserName(context),
            targetId = null,   // broadcast to all; auth/rescuers will display it
            type = MessageType.SOS,
            content = formattedContent,
            latitude = lat,
            longitude = lon,
            batteryLevel = BatteryHelper.getLevel(context),
            nearbyDevicesCount = connectedDevices,
            ttl = 10,           // extra hops for SOS
            deviceId = deviceId
        )
        router.sendMessage(msg)
        Log.d(TAG, "SOS sent to router: id=${msg.messageId}, formatted with address and metadata")
        Log.d(TAG, "SOS content: $formattedContent")
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
