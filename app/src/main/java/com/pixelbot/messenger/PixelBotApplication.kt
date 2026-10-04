package com.pixelbot.messenger

import android.app.Application
import com.pixelbot.messenger.data.PreferencesManager

class PixelBotApplication : Application() {
    companion object {
        lateinit var instance: PixelBotApplication
            private set
    }

    lateinit var prefs: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesManager(this)
        prefs.initDefaultPresetsIfEmpty()
    }
}
