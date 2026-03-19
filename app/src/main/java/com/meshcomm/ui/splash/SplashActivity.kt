package com.meshcomm.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.meshcomm.ui.home.HomeActivity
import com.meshcomm.ui.setup.SetupActivity
import com.meshcomm.utils.PrefsHelper

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val next = if (PrefsHelper.isSetupDone(this)) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, SetupActivity::class.java)
        }
        startActivity(next)
        finish()
    }
}
