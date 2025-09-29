package com.example.dermtect

import android.app.Application
import org.osmdroid.config.Configuration

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            // A clearer UA helps avoid tile blocking and aids debugging
            userAgentValue = "Dermtect/1.0 ($packageName)"
            // Load/persist OSMdroid settings & cache paths
            load(this@App, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
    }
}
