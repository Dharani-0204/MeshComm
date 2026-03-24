package com.meshcomm

import android.app.Application
import android.preference.PreferenceManager
import android.util.Log
import com.meshcomm.utils.NotificationHelper
import com.meshcomm.utils.PrefsHelper
import org.osmdroid.config.Configuration

class MeshCommApp : Application() {

    companion object {
        private const val TAG = "MeshCommApp"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize notification channels
        NotificationHelper.createChannels(this)

        // Initialize OSMDroid configuration for offline maps
        try {
            Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
            Configuration.getInstance().userAgentValue = packageName
            Log.d(TAG, "OSMDroid configured for offline maps")
        } catch (e: Exception) {
            Log.e(TAG, "OSMDroid configuration failed: ${e.message}")
        }

        // Initialize userId if not exists (for offline mesh networking)
        PrefsHelper.getUserId(this)

        Log.i(TAG, "MeshComm initialized successfully (offline mode)")
    }
}
