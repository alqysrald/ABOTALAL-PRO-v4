package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RouterDao {
    @Query("SELECT * FROM routers WHERE userId = :userId OR :userId = '' ORDER BY id DESC")
    fun getRoutersForUser(userId: String): Flow<List<RouterEntity>>

    @Query("SELECT * FROM routers ORDER BY id DESC")
    fun getAllRouters(): Flow<List<RouterEntity>>

    @Query("SELECT * FROM routers WHERE id = :id LIMIT 1")
    suspend fun getRouterById(id: Long): RouterEntity?

    @Query("SELECT * FROM routers WHERE userId = :userId OR :userId = ''")
    suspend fun getRoutersListForUser(userId: String): List<RouterEntity>

    @Query("SELECT * FROM routers")
    suspend fun getAllRoutersList(): List<RouterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouter(router: RouterEntity): Long

    @Update
    suspend fun updateRouter(router: RouterEntity)

    @Delete
    suspend fun deleteRouter(router: RouterEntity)

    @Query("DELETE FROM routers WHERE id = :id")
    suspend fun deleteRouterById(id: Long)

    @Query("SELECT COUNT(*) FROM routers WHERE userId = :userId OR :userId = ''")
    fun getRoutersCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM routers WHERE isOnline = 1 AND (userId = :userId OR :userId = '')")
    fun getOnlineRoutersCount(userId: String): Flow<Int>
}

@Dao
interface NetworkDeviceDao {
    @Query("SELECT * FROM network_devices WHERE userId = :userId OR :userId = '' ORDER BY id DESC")
    fun getDevicesForUser(userId: String): Flow<List<NetworkDeviceEntity>>

    @Query("SELECT * FROM network_devices ORDER BY id DESC")
    fun getAllDevices(): Flow<List<NetworkDeviceEntity>>

    @Query("SELECT * FROM network_devices WHERE userId = :userId OR :userId = ''")
    suspend fun getDevicesListForUser(userId: String): List<NetworkDeviceEntity>

    @Query("SELECT * FROM network_devices")
    suspend fun getAllDevicesList(): List<NetworkDeviceEntity>

    @Query("SELECT * FROM network_devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: Long): NetworkDeviceEntity?

    @Query("SELECT * FROM network_devices WHERE routerId = :routerId AND (userId = :userId OR :userId = '')")
    fun getDevicesByRouter(routerId: Long, userId: String): Flow<List<NetworkDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: NetworkDeviceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<NetworkDeviceEntity>)

    @Update
    suspend fun updateDevice(device: NetworkDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: NetworkDeviceEntity)

    @Query("DELETE FROM network_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)

    @Query("SELECT COUNT(*) FROM network_devices WHERE userId = :userId OR :userId = ''")
    fun getTotalDevicesCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM network_devices WHERE status = 'ONLINE' AND (userId = :userId OR :userId = '')")
    fun getOnlineDevicesCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM network_devices WHERE status = 'OFFLINE' AND (userId = :userId OR :userId = '')")
    fun getOfflineDevicesCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM network_devices WHERE status = 'MAINTENANCE' AND (userId = :userId OR :userId = '')")
    fun getMaintenanceDevicesCount(userId: String): Flow<Int>
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE userId = :userId OR :userId = '' ORDER BY timestamp DESC")
    fun getAlertsForUser(userId: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT COUNT(*) FROM alerts WHERE isRead = 0 AND (userId = :userId OR :userId = '')")
    fun getUnreadAlertsCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Query("UPDATE alerts SET isRead = 1 WHERE isRead = 0 AND (userId = :userId OR :userId = '')")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM alerts WHERE userId = :userId OR :userId = ''")
    suspend fun clearAllAlerts(userId: String)
}

@Dao
interface NetworkLogDao {
    @Query("SELECT * FROM network_logs WHERE userId = :userId OR :userId = '' ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(userId: String): Flow<List<NetworkLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NetworkLogEntity): Long

    @Query("DELETE FROM network_logs WHERE userId = :userId OR :userId = ''")
    suspend fun clearLogs(userId: String)
}

// ================= USER MANAGER DAO =================

@Dao
interface UserManagerDao {
    @Query("SELECT * FROM user_manager_users WHERE routerId = :routerId AND (userId = :userId OR :userId = '') ORDER BY id DESC")
    fun getUsersForRouter(routerId: Long, userId: String): Flow<List<UserManagerUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserManagerUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserManagerUserEntity>)

    @Update
    suspend fun updateUser(user: UserManagerUserEntity)

    @Delete
    suspend fun deleteUser(user: UserManagerUserEntity)

    @Query("DELETE FROM user_manager_users WHERE routerId = :routerId AND (userId = :userId OR :userId = '')")
    suspend fun clearUsersForRouter(routerId: Long, userId: String)

    @Query("SELECT * FROM user_manager_profiles WHERE routerId = :routerId AND (userId = :userId OR :userId = '')")
    fun getProfilesForRouter(routerId: Long, userId: String): Flow<List<UserManagerProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserManagerProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<UserManagerProfileEntity>)

    @Delete
    suspend fun deleteProfile(profile: UserManagerProfileEntity)
}

// ================= HOTSPOT DAO =================

@Dao
interface HotspotDao {
    @Query("SELECT * FROM hotspot_users WHERE routerId = :routerId AND (userId = :userId OR :userId = '') ORDER BY id DESC")
    fun getHotspotUsersForRouter(routerId: Long, userId: String): Flow<List<HotspotUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspotUser(user: HotspotUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspotUsers(users: List<HotspotUserEntity>)

    @Update
    suspend fun updateHotspotUser(user: HotspotUserEntity)

    @Delete
    suspend fun deleteHotspotUser(user: HotspotUserEntity)

    @Query("DELETE FROM hotspot_users WHERE routerId = :routerId AND (userId = :userId OR :userId = '')")
    suspend fun clearHotspotUsersForRouter(routerId: Long, userId: String)

    @Query("SELECT * FROM hotspot_active_sessions WHERE routerId = :routerId AND (userId = :userId OR :userId = '')")
    fun getActiveSessionsForRouter(routerId: Long, userId: String): Flow<List<HotspotActiveSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSessions(sessions: List<HotspotActiveSessionEntity>)

    @Query("DELETE FROM hotspot_active_sessions WHERE routerId = :routerId AND (userId = :userId OR :userId = '')")
    suspend fun clearActiveSessions(routerId: Long, userId: String)

    @Query("SELECT * FROM hotspot_profiles WHERE routerId = :routerId AND (userId = :userId OR :userId = '')")
    fun getHotspotProfilesForRouter(routerId: Long, userId: String): Flow<List<HotspotProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspotProfile(profile: HotspotProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspotProfiles(profiles: List<HotspotProfileEntity>)

    @Delete
    suspend fun deleteHotspotProfile(profile: HotspotProfileEntity)
}

// ================= TELEGRAM CONFIG DAO =================

@Dao
interface TelegramConfigDao {
    @Query("SELECT * FROM telegram_config WHERE userId = :userId LIMIT 1")
    fun getTelegramConfigForUser(userId: String): Flow<TelegramConfigEntity?>

    @Query("SELECT * FROM telegram_config WHERE userId = :userId LIMIT 1")
    suspend fun getTelegramConfigSync(userId: String): TelegramConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTelegramConfig(config: TelegramConfigEntity): Long
}

// ================= ADVANCED TELEGRAM BOT DAO =================

@Dao
interface AdvancedTelegramBotDao {
    @Query("SELECT * FROM advanced_telegram_bot WHERE userId = :userId LIMIT 1")
    fun getAdvancedConfigForUser(userId: String): Flow<AdvancedTelegramBotEntity?>

    @Query("SELECT * FROM advanced_telegram_bot WHERE userId = :userId LIMIT 1")
    suspend fun getAdvancedConfigSync(userId: String): AdvancedTelegramBotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAdvancedConfig(config: AdvancedTelegramBotEntity): Long

    @Delete
    suspend fun deleteAdvancedConfig(config: AdvancedTelegramBotEntity)

    @Query("DELETE FROM advanced_telegram_bot WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}

// ================= CUSTOM TELEGRAM BUTTONS DAO =================

@Dao
interface CustomTelegramButtonDao {
    @Query("SELECT * FROM custom_telegram_buttons WHERE userId = :userId OR userId = '' ORDER BY sortOrder ASC")
    fun getAllButtons(userId: String): Flow<List<TelegramCustomButtonEntity>>

    @Query("SELECT * FROM custom_telegram_buttons WHERE userId = :userId OR userId = '' ORDER BY sortOrder ASC")
    suspend fun getAllButtonsSync(userId: String): List<TelegramCustomButtonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveButton(button: TelegramCustomButtonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButtons(buttons: List<TelegramCustomButtonEntity>)

    @Delete
    suspend fun deleteButton(button: TelegramCustomButtonEntity)

    @Query("DELETE FROM custom_telegram_buttons WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

// ================= CARD TEMPLATE DAO =================

@Dao
interface CardTemplateDao {
    @Query("SELECT * FROM card_templates WHERE (userId = :userId OR :userId = '') AND systemType = :systemType")
    fun getTemplatesForSystem(userId: String, systemType: String): Flow<List<CardTemplateEntity>>

    @Query("SELECT * FROM card_templates WHERE (userId = :userId OR :userId = '') AND systemType = :systemType AND profileName = :profileName LIMIT 1")
    fun getTemplateForProfile(userId: String, systemType: String, profileName: String): Flow<CardTemplateEntity?>

    @Query("SELECT * FROM card_templates WHERE (userId = :userId OR :userId = '') AND systemType = :systemType AND profileName = :profileName LIMIT 1")
    suspend fun getTemplateForProfileSync(userId: String, systemType: String, profileName: String): CardTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTemplate(template: CardTemplateEntity): Long

    @Delete
    suspend fun deleteTemplate(template: CardTemplateEntity)
}
