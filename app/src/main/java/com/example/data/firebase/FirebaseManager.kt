package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.AlertSeverity
import com.example.data.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AiProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Google Gemini Pro/Flash",
    val type: String = "GEMINI", // "GEMINI", "OPENAI", "CUSTOM_PROXY", "OLLAMA"
    val apiUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    val apiKey: String = "",
    val modelName: String = "gemini-2.5-flash",
    val systemPrompt: String = "أنت خبير شبكات المايكروتيك والمساعد الذكي المعتمد لمنظومة TALAL PRO. تساعد المشرف في فحص الراوترات وحل المشاكل وإصدار الأوامر بدقة واحترافية.",
    val isEnabled: Boolean = true,
    val isDefault: Boolean = true,
    val lastTestStatus: String = "جاهز ومتصل ✓",
    val lastTestTimestamp: Long = 0L,
    val lastLatencyMs: Long = 0L
)

data class AIAssistantPermissions(
    val allowRouterCommands: Boolean = true,
    val allowReboot: Boolean = true,
    val allowUserManagerOps: Boolean = true,
    val allowHotspotOps: Boolean = true,
    val systemPromptOverride: String = "",
    val aiModelMode: String = "gemini-flash" // or "pro"
)

data class FirebaseUserAccount(
    val id: String = "",
    val email: String = "",
    val password: String = "",
    val name: String = "", // الاسم الكامل
    val phone: String = "", // رقم الهاتف (إلزامي)
    val governorate: String = "", // المحافظة
    val city: String = "", // المدينة
    val networkName: String = "", // اسم الشبكة
    val networkLocation: String = "", // موقع الشبكة (اختياري / إحداثيات)
    val role: String = "user", // "admin", "moderator", "user"
    val status: String = "active", // "active", "suspended", "banned", "pending_activation"
    val isActivated: Boolean = true, // مفعل بكود التفعيل
    val licenseRequestCode: String = "", // كود طلب الترخيص
    val activationCode: String = "", // كود التفعيل الممنوح
    val isLifetime: Boolean = false,
    val expiryTimestamp: Long = 0L,
    val trialDays: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = 0L,
    val maxRouters: Int = 10,
    val maxDevices: Int = 50,
    val maxAllowedDevices: Int = 1,
    val allowedAccountsOnDevice: Int = 1, // السماح بحسابين على نفس الجهاز
    val isDeviceLocked: Boolean = true,
    val boundDeviceId: String = "",
    val deviceFingerprint: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val directMessage: String? = null,
    val notes: String = ""
) {
    val isExpired: Boolean
        get() = !isLifetime && expiryTimestamp > 0 && System.currentTimeMillis() > expiryTimestamp

    val statusArabic: String
        get() = when {
            !isActivated || status == "pending_activation" -> "بانتظار كود التفعيل 🔑"
            status == "banned" -> "محظور ⛔"
            status == "suspended" -> "معلّق ⏸"
            isExpired -> "منتهي الصلاحية ⌛"
            isLifetime -> "مدى الحياة ♾️"
            else -> "نشط ✓"
        }
}

data class FirebaseAppConfig(
    val freeTrialDaysDefault: Int = 30,
    val isTrialEnabled: Boolean = true,
    val systemNotice: String = "",
    val isMaintenanceMode: Boolean = false,
    val enabledTabs: List<String> = listOf("DASHBOARD", "ROUTERS", "DEVICES", "USER_MANAGER", "HOTSPOT", "CARD_DESIGNER", "MAP", "TELEGRAM", "TOOLS", "ASSISTANT", "ISSUES"),
    val developerName: String = "عبدالحميد داوؤد",
    val supportPhones: String = "778215553 - 700897775",
    val whatsappSupportNumber: String = "778215553",
    val mainAdminEmail: String = "abdualhamid100@gmail.com",
    val aiPermissions: AIAssistantPermissions = AIAssistantPermissions(),
    val aiProviders: List<AiProviderConfig> = listOf(
        AiProviderConfig(
            id = "gemini_default",
            name = "Google Gemini AI (الافتراضي)",
            type = "GEMINI",
            apiUrl = "https://generativelanguage.googleapis.com/v1beta",
            modelName = "gemini-2.5-flash",
            isEnabled = true,
            isDefault = true
        )
    ),
    val moderators: List<String> = emptyList() // Emails of authorized sub-admins
)

class FirebaseManager(private val context: Context) {
    private val tag = "FirebaseManager"
    private val databaseUrl = "https://talal-pro-default-rtdb.firebaseio.com"
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun sanitizeEmailKey(email: String): String {
        return email.lowercase()
            .replace(".", "_")
            .replace("@", "_at_")
            .replace("#", "")
            .replace("$", "")
            .replace("[", "")
            .replace("]", "")
    }

    suspend fun getAppConfig(): FirebaseAppConfig = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$databaseUrl/app_config.json")
                .get()
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val aiJson = json.optJSONObject("aiPermissions")
                val aiPerms = if (aiJson != null) {
                    AIAssistantPermissions(
                        allowRouterCommands = aiJson.optBoolean("allowRouterCommands", true),
                        allowReboot = aiJson.optBoolean("allowReboot", true),
                        allowUserManagerOps = aiJson.optBoolean("allowUserManagerOps", true),
                        allowHotspotOps = aiJson.optBoolean("allowHotspotOps", true),
                        systemPromptOverride = aiJson.optString("systemPromptOverride", ""),
                        aiModelMode = aiJson.optString("aiModelMode", "gemini-flash")
                    )
                } else AIAssistantPermissions()

                val providersList = mutableListOf<AiProviderConfig>()
                val provArray = json.optJSONArray("aiProviders")
                if (provArray != null) {
                    for (i in 0 until provArray.length()) {
                        val pObj = provArray.optJSONObject(i) ?: continue
                        providersList.add(
                            AiProviderConfig(
                                id = pObj.optString("id", UUID.randomUUID().toString()),
                                name = pObj.optString("name", "Custom AI Agent"),
                                type = pObj.optString("type", "GEMINI"),
                                apiUrl = pObj.optString("apiUrl", "https://generativelanguage.googleapis.com/v1beta"),
                                apiKey = pObj.optString("apiKey", ""),
                                modelName = pObj.optString("modelName", "gemini-2.5-flash"),
                                systemPrompt = pObj.optString("systemPrompt", ""),
                                isEnabled = pObj.optBoolean("isEnabled", true),
                                isDefault = pObj.optBoolean("isDefault", false),
                                lastTestStatus = pObj.optString("lastTestStatus", "متصل ✓")
                            )
                        )
                    }
                }
                if (providersList.isEmpty()) {
                    providersList.add(AiProviderConfig(id = "gemini_default", name = "Google Gemini AI (الافتراضي)", type = "GEMINI", modelName = "gemini-2.5-flash", isDefault = true))
                }

                val modsList = mutableListOf<String>()
                val modsArr = json.optJSONArray("moderators")
                if (modsArr != null) {
                    for (i in 0 until modsArr.length()) {
                        modsList.add(modsArr.optString(i))
                    }
                }

                FirebaseAppConfig(
                    freeTrialDaysDefault = json.optInt("freeTrialDaysDefault", 30),
                    isTrialEnabled = json.optBoolean("isTrialEnabled", true),
                    systemNotice = json.optString("systemNotice", ""),
                    isMaintenanceMode = json.optBoolean("isMaintenanceMode", false),
                    developerName = json.optString("developerName", "عبدالحميد داوؤد"),
                    supportPhones = json.optString("supportPhones", "778215553 - 700897775"),
                    whatsappSupportNumber = json.optString("whatsappSupportNumber", "778215553"),
                    mainAdminEmail = json.optString("mainAdminEmail", "abdualhamid100@gmail.com"),
                    aiPermissions = aiPerms,
                    aiProviders = providersList,
                    moderators = modsList
                )
            } else {
                FirebaseAppConfig()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get app config", e)
            FirebaseAppConfig()
        }
    }

    suspend fun saveAppConfig(config: FirebaseAppConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("freeTrialDaysDefault", config.freeTrialDaysDefault)
                put("isTrialEnabled", config.isTrialEnabled)
                put("systemNotice", config.systemNotice)
                put("isMaintenanceMode", config.isMaintenanceMode)
                put("developerName", config.developerName)
                put("supportPhones", config.supportPhones)
                put("whatsappSupportNumber", config.whatsappSupportNumber)
                put("mainAdminEmail", config.mainAdminEmail)

                val aiObj = JSONObject().apply {
                    put("allowRouterCommands", config.aiPermissions.allowRouterCommands)
                    put("allowReboot", config.aiPermissions.allowReboot)
                    put("allowUserManagerOps", config.aiPermissions.allowUserManagerOps)
                    put("allowHotspotOps", config.aiPermissions.allowHotspotOps)
                    put("systemPromptOverride", config.aiPermissions.systemPromptOverride)
                    put("aiModelMode", config.aiPermissions.aiModelMode)
                }
                put("aiPermissions", aiObj)

                val provArr = JSONArray()
                config.aiProviders.forEach { prov ->
                    provArr.put(JSONObject().apply {
                        put("id", prov.id)
                        put("name", prov.name)
                        put("type", prov.type)
                        put("apiUrl", prov.apiUrl)
                        put("apiKey", prov.apiKey)
                        put("modelName", prov.modelName)
                        put("systemPrompt", prov.systemPrompt)
                        put("isEnabled", prov.isEnabled)
                        put("isDefault", prov.isDefault)
                        put("lastTestStatus", prov.lastTestStatus)
                        put("lastTestTimestamp", prov.lastTestTimestamp)
                        put("lastLatencyMs", prov.lastLatencyMs)
                    })
                }
                put("aiProviders", provArr)

                val modsArr = JSONArray()
                config.moderators.forEach { modsArr.put(it) }
                put("moderators", modsArr)
            }

            val req = Request.Builder()
                .url("$databaseUrl/app_config.json")
                .put(json.toString().toRequestBody(jsonMediaType))
                .build()
            val resp = client.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e(tag, "Failed to save app config", e)
            false
        }
    }

    suspend fun getAllUsers(): List<FirebaseUserAccount> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$databaseUrl/users.json")
                .get()
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            val list = mutableListOf<FirebaseUserAccount>()

            if (resp.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val uObj = json.optJSONObject(k) ?: continue
                    list.add(parseUserJson(k, uObj))
                }
            }

            // Ensure main Admin always exists
            val adminEmail = "abdualhamid100@gmail.com"
            if (list.none { it.email.equals(adminEmail, ignoreCase = true) }) {
                val masterAdmin = FirebaseUserAccount(
                    id = sanitizeEmailKey(adminEmail),
                    email = adminEmail,
                    password = "738104363a",
                    name = "عبدالحميد داوؤد (المدير العام)",
                    phone = "778215553",
                    role = "admin",
                    status = "active",
                    isActivated = true,
                    isLifetime = true,
                    maxRouters = 999,
                    maxDevices = 999,
                    maxAllowedDevices = 99,
                    allowedAccountsOnDevice = 99
                )
                saveUser(masterAdmin)
                list.add(masterAdmin)
            }

            list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get all users", e)
            emptyList()
        }
    }

    suspend fun getUserByEmail(email: String): FirebaseUserAccount? = withContext(Dispatchers.IO) {
        try {
            val key = sanitizeEmailKey(email)
            val req = Request.Builder()
                .url("$databaseUrl/users/$key.json")
                .get()
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                parseUserJson(key, json)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get user by email", e)
            null
        }
    }

    // REGISTRATION WITH DUPLICATION CHECK
    suspend fun registerNewUser(
        fullName: String,
        governorate: String,
        city: String,
        networkName: String,
        networkLocation: String,
        phone: String,
        email: String,
        password: String
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanPhone = phone.trim()
            val existing = getAllUsers()

            if (existing.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
                return@withContext Pair(false, "البريد الإلكتروني مسجل مسبقاً! يرجى استخدام بريد آخر.")
            }

            if (existing.any { it.phone == cleanPhone && cleanPhone.isNotBlank() }) {
                return@withContext Pair(false, "رقم الهاتف مسجل مسبقاً لحساب آخر!")
            }

            val appConfig = getAppConfig()
            val reqCode = DeviceUtils.generateLicenseRequestCode(context, cleanEmail, cleanPhone)
            val fp = DeviceUtils.getDeviceFingerprint(context)
            val devModel = DeviceUtils.getDeviceModel()

            val trialDays = if (appConfig.isTrialEnabled) appConfig.freeTrialDaysDefault else 30
            val expTime = System.currentTimeMillis() + (trialDays.toLong() * 24 * 60 * 60 * 1000L)

            val newUser = FirebaseUserAccount(
                id = sanitizeEmailKey(cleanEmail),
                email = cleanEmail,
                password = password.trim(),
                name = fullName.trim(),
                phone = cleanPhone,
                governorate = governorate.trim(),
                city = city.trim(),
                networkName = networkName.trim(),
                networkLocation = networkLocation.trim(),
                role = "user",
                status = "pending_activation", // Pending activation code from admin
                isActivated = false,
                licenseRequestCode = reqCode,
                activationCode = "",
                trialDays = trialDays,
                expiryTimestamp = expTime,
                boundDeviceId = fp,
                deviceFingerprint = fp,
                deviceModel = devModel,
                androidVersion = DeviceUtils.getAndroidVersion(),
                maxAllowedDevices = 1,
                allowedAccountsOnDevice = 1
            )

            val ok = saveUser(newUser)
            if (ok) {
                Pair(true, null)
            } else {
                Pair(false, "فشل حفظ الحساب في السيرفر. تأكد من اتصال الإنترنت.")
            }
        } catch (e: Exception) {
            Pair(false, "خطأ أثناء التسجيل: ${e.localizedMessage}")
        }
    }

    // ACTIVATION CODE VERIFICATION
    suspend fun activateUserLicense(email: String, inputCode: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val user = getUserByEmail(email) ?: return@withContext Pair(false, "الحساب غير موجود")
            val currentFp = DeviceUtils.getDeviceFingerprint(context)

            // Verify activation code
            val isValid = DeviceUtils.verifyActivationCode(user.licenseRequestCode, inputCode) ||
                    user.activationCode.equals(inputCode.trim(), ignoreCase = true)

            if (!isValid) {
                return@withContext Pair(false, "كود التفعيل غير صحيح! يرجى التواصل مع الإدارة عبر الواتساب لتزويدك بالكود الصحيح.")
            }

            val updated = user.copy(
                isActivated = true,
                status = "active",
                activationCode = inputCode.trim(),
                boundDeviceId = currentFp,
                deviceFingerprint = currentFp,
                deviceModel = DeviceUtils.getDeviceModel()
            )
            saveUser(updated)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "خطأ في التفعيل: ${e.localizedMessage}")
        }
    }

    suspend fun saveUser(user: FirebaseUserAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = sanitizeEmailKey(user.email)
            val json = JSONObject().apply {
                put("id", key)
                put("email", user.email)
                put("password", user.password)
                put("name", user.name)
                put("phone", user.phone)
                put("governorate", user.governorate)
                put("city", user.city)
                put("networkName", user.networkName)
                put("networkLocation", user.networkLocation)
                put("role", user.role)
                put("status", user.status)
                put("isActivated", user.isActivated)
                put("licenseRequestCode", user.licenseRequestCode)
                put("activationCode", user.activationCode)
                put("isLifetime", user.isLifetime)
                put("expiryTimestamp", user.expiryTimestamp)
                put("trialDays", user.trialDays)
                put("createdAt", user.createdAt)
                put("lastLogin", user.lastLogin)
                put("maxRouters", user.maxRouters)
                put("maxDevices", user.maxDevices)
                put("maxAllowedDevices", user.maxAllowedDevices)
                put("allowedAccountsOnDevice", user.allowedAccountsOnDevice)
                put("isDeviceLocked", user.isDeviceLocked)
                put("boundDeviceId", user.boundDeviceId)
                put("deviceFingerprint", user.deviceFingerprint)
                put("deviceModel", user.deviceModel)
                put("androidVersion", user.androidVersion)
                put("directMessage", user.directMessage ?: "")
                put("notes", user.notes)
            }

            val req = Request.Builder()
                .url("$databaseUrl/users/$key.json")
                .put(json.toString().toRequestBody(jsonMediaType))
                .build()
            val resp = client.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e(tag, "Failed to save user", e)
            false
        }
    }

    suspend fun deleteUser(user: FirebaseUserAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = sanitizeEmailKey(user.email)
            val req = Request.Builder()
                .url("$databaseUrl/users/$key.json")
                .delete()
                .build()
            val resp = client.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete user", e)
            false
        }
    }

    suspend fun unbindUserDevice(user: FirebaseUserAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            val updated = user.copy(
                boundDeviceId = "",
                deviceFingerprint = "",
                deviceModel = ""
            )
            saveUser(updated)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleUserDeviceLock(user: FirebaseUserAccount, lock: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val updated = user.copy(isDeviceLocked = lock)
            saveUser(updated)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun testAiProviderConnection(provider: AiProviderConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            if (provider.type == "GEMINI") {
                val apiKey = provider.apiKey
                val url = if (apiKey.isNotBlank()) {
                    "${provider.apiUrl}/models/${provider.modelName}:generateContent?key=$apiKey"
                } else {
                    "${provider.apiUrl}/models/${provider.modelName}"
                }
                val pingReq = Request.Builder().url(url).get().build()
                val resp = client.newCall(pingReq).execute()
                val latency = System.currentTimeMillis() - start
                if (resp.isSuccessful || resp.code == 400 || resp.code == 404) {
                    Pair(true, "✅ الاتصال مع مزود AI ناجح ومستقر (${latency}ms)")
                } else {
                    Pair(false, "❌ خطأ في الاتصال بالمزود (${resp.code}): ${resp.message}")
                }
            } else {
                val req = Request.Builder().url(provider.apiUrl).get().build()
                val resp = client.newCall(req).execute()
                val latency = System.currentTimeMillis() - start
                Pair(true, "✅ استجابة الخادم جاهزة (${latency}ms)")
            }
        } catch (e: Exception) {
            Pair(false, "❌ تعذر الاتصال بمزود الذكاء: ${e.localizedMessage}")
        }
    }

    // AUTHENTICATION
    suspend fun authenticate(emailOrUser: String, pass: String): Pair<FirebaseUserAccount?, String?> = withContext(Dispatchers.IO) {
        try {
            val cleanInput = emailOrUser.trim().lowercase()
            val cleanPass = pass.trim()
            val currentFp = DeviceUtils.getDeviceFingerprint(context)

            // Special fallback for Master Admin
            if ((cleanInput == "abdualhamid100@gmail.com" || cleanInput == "admin" || cleanInput == "778215553") && cleanPass == "738104363a") {
                val admin = FirebaseUserAccount(
                    id = "abdualhamid100_at_gmail_com",
                    email = "abdualhamid100@gmail.com",
                    password = "738104363a",
                    name = "عبدالحميد داوؤد (المدير العام)",
                    phone = "778215553",
                    role = "admin",
                    status = "active",
                    isActivated = true,
                    isLifetime = true,
                    maxRouters = 999,
                    maxDevices = 999,
                    maxAllowedDevices = 99,
                    allowedAccountsOnDevice = 99,
                    boundDeviceId = currentFp,
                    deviceFingerprint = currentFp
                )
                saveUser(admin)
                return@withContext Pair(admin, null)
            }

            val allUsers = getAllUsers()
            val matchedUser = allUsers.firstOrNull { u ->
                (u.email.equals(cleanInput, ignoreCase = true) ||
                 u.phone == cleanInput ||
                 u.name.equals(emailOrUser.trim(), ignoreCase = true) ||
                 u.id.equals(cleanInput, ignoreCase = true)) &&
                u.password == cleanPass
            }

            if (matchedUser == null) {
                return@withContext Pair(null, "اسم المستخدم أو كلمة المرور غير صحيحة")
            }

            if (matchedUser.status == "banned") {
                return@withContext Pair(null, "تم حظر هذا الحساب من قبل إدارة التطبيق.")
            }

            if (matchedUser.status == "suspended") {
                return@withContext Pair(null, "هذا الحساب معلق مؤقتاً. يرجى التواصل مع الإدارة.")
            }

            // Check device lock if active user and device is locked
            if (matchedUser.role != "admin" && matchedUser.isDeviceLocked && matchedUser.boundDeviceId.isNotBlank()) {
                if (matchedUser.boundDeviceId != currentFp) {
                    return@withContext Pair(null, "تنبيه أمني: هذا الحساب مرخص ومقترن بهاتف آخر! لا يمكن استخدام الحساب على أكثر من جهاز.")
                }
            }

            // If active and device not yet bound, bind now
            var updatedUser = matchedUser
            if (updatedUser.boundDeviceId.isBlank()) {
                updatedUser = updatedUser.copy(
                    boundDeviceId = currentFp,
                    deviceFingerprint = currentFp,
                    deviceModel = DeviceUtils.getDeviceModel(),
                    androidVersion = DeviceUtils.getAndroidVersion()
                )
            }

            // Update last login
            updatedUser = updatedUser.copy(lastLogin = System.currentTimeMillis())
            saveUser(updatedUser)

            Pair(updatedUser, null)
        } catch (e: Exception) {
            Log.e(tag, "Authentication error", e)
            Pair(null, "خطأ في تسجيل الدخول: ${e.localizedMessage}")
        }
    }

    private fun parseUserJson(key: String, json: JSONObject): FirebaseUserAccount {
        return FirebaseUserAccount(
            id = key,
            email = json.optString("email", key),
            password = json.optString("password", ""),
            name = json.optString("name", ""),
            phone = json.optString("phone", ""),
            governorate = json.optString("governorate", ""),
            city = json.optString("city", ""),
            networkName = json.optString("networkName", ""),
            networkLocation = json.optString("networkLocation", ""),
            role = json.optString("role", "user"),
            status = json.optString("status", "active"),
            isActivated = json.optBoolean("isActivated", true),
            licenseRequestCode = json.optString("licenseRequestCode", ""),
            activationCode = json.optString("activationCode", ""),
            isLifetime = json.optBoolean("isLifetime", false),
            expiryTimestamp = json.optLong("expiryTimestamp", 0L),
            trialDays = json.optInt("trialDays", 30),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            lastLogin = json.optLong("lastLogin", 0L),
            maxRouters = json.optInt("maxRouters", 10),
            maxDevices = json.optInt("maxDevices", 50),
            maxAllowedDevices = json.optInt("maxAllowedDevices", 1),
            allowedAccountsOnDevice = json.optInt("allowedAccountsOnDevice", 1),
            isDeviceLocked = json.optBoolean("isDeviceLocked", true),
            boundDeviceId = json.optString("boundDeviceId", ""),
            deviceFingerprint = json.optString("deviceFingerprint", ""),
            deviceModel = json.optString("deviceModel", ""),
            androidVersion = json.optString("androidVersion", ""),
            directMessage = json.optString("directMessage").takeIf { it.isNotBlank() },
            notes = json.optString("notes", "")
        )
    }
}
