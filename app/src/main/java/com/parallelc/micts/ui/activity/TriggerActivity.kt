package com.parallelc.micts.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.parallelc.micts.config.AppConfig

class TriggerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(AppConfig.CONFIG_NAME, MODE_PRIVATE)
        val vibrate = prefs.getBoolean(AppConfig.KEY_VIBRATE, false)
        triggerCircleToSearch(1, this, vibrate)
        finish()
    }
}