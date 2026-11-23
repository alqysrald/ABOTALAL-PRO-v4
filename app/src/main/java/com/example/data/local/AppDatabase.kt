package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        RouterEntity::class,
        NetworkDeviceEntity::class,
        AlertEntity::class,
        NetworkLogEntity::class,
        UserManagerUserEntity::class,
        UserManagerProfileEntity::class,
        HotspotUserEntity::class,
        HotspotProfileEntity::class,
        HotspotActiveSessionEntity::class,
        TelegramConfigEntity::class,
        CardTemplateEntity::class,
        AdvancedTelegramBotEntity::class,
        TelegramCustomButtonEntity::class,
        CardBatchEntity::class,
        CardBatchItemEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routerDao(): RouterDao
    abstract fun networkDeviceDao(): NetworkDeviceDao
    abstract fun alertDao(): AlertDao
    abstract fun networkLogDao(): NetworkLogDao
    abstract fun userManagerDao(): UserManagerDao
    abstract fun hotspotDao(): HotspotDao
    abstract fun telegramConfigDao(): TelegramConfigDao
    abstract fun cardTemplateDao(): CardTemplateDao
    abstract fun advancedTelegramBotDao(): AdvancedTelegramBotDao
    abstract fun customTelegramButtonDao(): CustomTelegramButtonDao
    abstract fun cardBatchDao(): CardBatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "talal_pro_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
