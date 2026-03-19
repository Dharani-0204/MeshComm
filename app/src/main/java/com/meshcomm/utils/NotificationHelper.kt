package com.meshcomm.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.meshcomm.R
import com.meshcomm.ui.home.HomeActivity

object NotificationHelper {
    const val CHANNEL_MESH = "mesh_service"
    const val CHANNEL_SOS  = "sos_alerts"
    const val CHANNEL_MSG  = "messages"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_MESH, "Mesh Service", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Background mesh connectivity" })
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_SOS, "SOS Alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Emergency SOS alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            enableLights(true)
            lightColor = android.graphics.Color.RED
        })
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_MSG, "Messages", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "New mesh messages" })
    }

    fun buildServiceNotification(context: Context, peerCount: Int): Notification {
        val intent = Intent(context, HomeActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL_MESH)
            .setContentTitle("MeshComm Active")
            .setContentText("Connected to $peerCount device(s)")
            .setSmallIcon(R.drawable.ic_mesh)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    fun showSOSNotification(context: Context, senderName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("open_tab", "sos")
        }
        val pi = PendingIntent.getActivity(context, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(context, CHANNEL_SOS)
            .setContentTitle("🚨 SOS ALERT")
            .setContentText("Emergency from $senderName")
            .setSmallIcon(R.drawable.ic_sos)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(android.graphics.Color.RED)
            .setColorized(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    fun showMessageNotification(context: Context, senderName: String, preview: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, HomeActivity::class.java)
        val pi = PendingIntent.getActivity(context, 2, intent, PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(context, CHANNEL_MSG)
            .setContentTitle("New message from $senderName")
            .setContentText(preview)
            .setSmallIcon(R.drawable.ic_message)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}
