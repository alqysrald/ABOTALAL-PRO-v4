package com.example

import android.app.Application
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.repository.NetworkRepository
import com.example.notifications.NotificationHelper
import com.example.sound.SoundEffectManager

class TalalProApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val soundManager by lazy { SoundEffectManager(this) }
    val firebaseManager by lazy { FirebaseManager(this) }
    val repository by lazy {
        NetworkRepository(
            routerDao = database.routerDao(),
            networkDeviceDao = database.networkDeviceDao(),
            alertDao = database.alertDao(),
            networkLogDao = database.networkLogDao(),
            userManagerDao = database.userManagerDao(),
            hotspotDao = database.hotspotDao(),
            telegramConfigDao = database.telegramConfigDao(),
            cardTemplateDao = database.cardTemplateDao(),
            advancedTelegramBotDao = database.advancedTelegramBotDao(),
            customTelegramButtonDao = database.customTelegramButtonDao(),
            soundEffectManager = soundManager,
            appContext = this
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        soundManager.release()
    }
}
