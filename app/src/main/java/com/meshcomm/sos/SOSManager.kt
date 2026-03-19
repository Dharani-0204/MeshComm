package com.meshcomm.sos

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.location.LocationProvider
import com.meshcomm.mesh.MessageRouter
import com.meshcomm.mesh.PeerRegistry
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper

class SOSManager(
    private val context: Context,
    private val router: MessageRouter,
    private val locationProvider: LocationProvider
) {
    fun sendSOS(customMessage: String = "EMERGENCY! I need help!") {
        val (lat, lon) = locationProvider.getLastLatLon()
        val msg = Message(
            senderId = PrefsHelper.getUserId(context),
            senderName = PrefsHelper.getUserName(context),
            targetId = null,   // broadcast to all; auth/rescuers will display it
            type = MessageType.SOS,
            content = customMessage,
            latitude = lat,
            longitude = lon,
            batteryLevel = BatteryHelper.getLevel(context),
            nearbyDevicesCount = PeerRegistry.getConnectedCount(),
            ttl = 10           // extra hops for SOS
        )
        router.sendMessage(msg)
        triggerLocalAlert()
    }

    /** Play alert + vibrate on the RECEIVING device */
    fun triggerSOSAlert() {
        triggerLocalAlert()
    }

    private fun triggerLocalAlert() {
        // Vibration pattern: SOS  (... --- ...)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 100, 500, 100, 500, 300, 200, 100, 200, 100, 200)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))

        // Ringtone
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ring = RingtoneManager.getRingtone(context, notification)
            ring.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ring.play()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
