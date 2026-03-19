package com.meshcomm.mesh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager

class WiFiDirectBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // MeshService listens to the same intent; forward via LocalBroadcast
        val localIntent = Intent("com.meshcomm.WIFI_P2P_EVENT").apply {
            action = intent.action
            replaceExtras(intent.extras)
        }
        context.sendBroadcast(localIntent)
    }
}
