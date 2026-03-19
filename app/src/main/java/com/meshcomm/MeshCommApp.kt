package com.meshcomm

import android.app.Application
import com.meshcomm.utils.NotificationHelper

class MeshCommApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
