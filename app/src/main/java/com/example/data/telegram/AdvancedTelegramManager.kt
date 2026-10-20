package com.example.data.telegram

import android.util.Log
import com.example.data.model.SuspiciousCardSharingAlert
import com.example.data.model.TelegramCustomButtonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AdvancedTelegramManager {
    private val tag = "AdvancedTelegramMgr"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testBotToken(token: String): TelegramBotInfo = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext TelegramBotInfo(isSuccess = false, errorMessage = "رمز البوت فارغ")
        }
        try {
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/getMe")
                .get()
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                if (json.optBoolean("ok", false)) {
                    val result = json.getJSONObject("result")
                    val id = result.optLong("id")
                    val name = result.optString("first_name", "Bot")
                    val username = result.optString("username", "")
                    return@withContext TelegramBotInfo(isSuccess = true, botId = id, botName = name, username = username)
                } else {
                    val desc = json.optString("description", "رمز البوت غير صالح")
                    return@withContext TelegramBotInfo(isSuccess = false, errorMessage = desc)
                }
            } else {
                return@withContext TelegramBotInfo(isSuccess = false, errorMessage = "فشل الاتصال بـ Telegram API (رمز: ${resp.code})")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to verify token", e)
            return@withContext TelegramBotInfo(isSuccess = false, errorMessage = e.localizedMessage ?: "خطأ في الشبكة")
        }
    }

    suspend fun getUpdates(token: String, offset: Long = 0, timeoutSeconds: Int = 10): List<TelegramIncomingUpdate> = withContext(Dispatchers.IO) {
        val updates = mutableListOf<TelegramIncomingUpdate>()
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return@withContext updates

        try {
            val url = if (offset > 0) {
                "https://api.telegram.org/bot$cleanToken/getUpdates?offset=$offset&timeout=$timeoutSeconds"
            } else {
                "https://api.telegram.org/bot$cleanToken/getUpdates?timeout=$timeoutSeconds"
            }
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                if (json.optBoolean("ok", false)) {
                    val results = json.getJSONArray("result")
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val uId = item.optLong("update_id")
                        val msg = item.optJSONObject("message") ?: item.optJSONObject("channel_post")
                        val callback = item.optJSONObject("callback_query")

                        if (msg != null) {
                            val chat = msg.optJSONObject("chat")
                            val chatId = chat?.opt("id")?.toString() ?: ""
                            val text = msg.optString("text", "")
                            val sender = msg.optJSONObject("from")?.optString("first_name") ?: "مستخدم"
                            if (chatId.isNotBlank() && text.isNotBlank()) {
                                updates.add(TelegramIncomingUpdate(uId, chatId, text, sender))
                            }
                        } else if (callback != null) {
                            val msgObj = callback.optJSONObject("message")
                            val chat = msgObj?.optJSONObject("chat")
                            val chatId = chat?.opt("id")?.toString() ?: ""
                            val data = callback.optString("data", "")
                            val sender = callback.optJSONObject("from")?.optString("first_name") ?: "مستخدم"
                            if (chatId.isNotBlank() && data.isNotBlank()) {
                                updates.add(TelegramIncomingUpdate(uId, chatId, data, sender))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Polling error", e)
        }
        updates
    }

    suspend fun sendMessage(
        token: String,
        chatId: String,
        message: String,
        parseMode: String = "HTML"
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val cleanChat = chatId.trim()
        if (cleanToken.isBlank() || cleanChat.isBlank()) {
            return@withContext Pair(false, "يجب إدخال التوكن ومعرف المحادثة Chat ID")
        }
        try {
            val json = JSONObject().apply {
                put("chat_id", cleanChat)
                put("text", message)
                put("parse_mode", parseMode)
            }
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/sendMessage")
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank()) {
                val resJson = JSONObject(body)
                if (resJson.optBoolean("ok", false)) {
                    return@withContext Pair(true, null)
                } else {
                    return@withContext Pair(false, resJson.optString("description", "فشل الإرسال"))
                }
            } else {
                return@withContext Pair(false, "فشل الإرسال (رمز: ${resp.code})")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message", e)
            return@withContext Pair(false, e.localizedMessage ?: "خطأ في الشبكة")
        }
    }

    suspend fun sendKeyboard(
        token: String,
        chatId: String,
        text: String,
        keyboard: JSONArray
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val cleanChat = chatId.trim()
        try {
            val replyMarkup = JSONObject().apply {
                put("keyboard", keyboard)
                put("resize_keyboard", true)
                put("is_persistent", true)
            }
            val json = JSONObject().apply {
                put("chat_id", cleanChat)
                put("text", text)
                put("parse_mode", "HTML")
                put("reply_markup", replyMarkup)
            }
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/sendMessage")
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank()) {
                val resJson = JSONObject(body)
                if (resJson.optBoolean("ok", false)) {
                    return@withContext Pair(true, null)
                } else {
                    return@withContext Pair(false, resJson.optString("description", "فشل إرسال لوحة الأزرار"))
                }
            }
            return@withContext Pair(false, "رمز الاستجابة: ${resp.code}")
        } catch (e: Exception) {
            return@withContext Pair(false, e.localizedMessage)
        }
    }

    suspend fun sendMainAdvancedKeyboard(
        token: String,
        chatId: String,
        routerName: String,
        customButtons: List<TelegramCustomButtonEntity> = emptyList()
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val keyboard = JSONArray().apply {
            // Row 1
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "📋 القائمة الرئيسية") })
                put(JSONObject().apply { put("text", "🟢 الحالة المباشرة") })
            })
            // Row 2: Cards Management
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🎫 الكروت") })
                put(JSONObject().apply { put("text", "👥 المتصلين الآن") })
            })
            // Row 3: Suspicious Sharing Check & Profiles
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "⚠️ فحص الاشتباه") })
                put(JSONObject().apply { put("text", "📦 باقات الشبكة") })
            })
            // Row 4: Distribution Units & Ports
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "📡 قطع التوزيع") })
                put(JSONObject().apply { put("text", "🔌 منافذ الشبكة") })
            })
            // Row 5: Port Traffic & Performance
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "📊 استهلاك المنافذ") })
                put(JSONObject().apply { put("text", "📈 ملخص الاداء") })
            })
            // Row 6: Backup & Optimize
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "💾 نسخة احتياطية") })
                put(JSONObject().apply { put("text", "⚡ تحسين الشبكة") })
            })

            // Dynamic Custom User Buttons
            val enabledCustom = customButtons.filter { it.isEnabled && !it.isBuiltIn }
            enabledCustom.chunked(2).forEach { rowList ->
                put(JSONArray().apply {
                    rowList.forEach { btn ->
                        put(JSONObject().apply { put("text", btn.title) })
                    }
                })
            }
        }

        val welcomeText = """
            ⚡ <b>مرحباً بك في بوت TALAL PRO المتقدم (Cloud Engine)</b>
            ━━━━━━━━━━━━━━━━━━
            📡 <b>الراوتر المتصل:</b> $routerName
            
            يمكنك إدارة الراوتر، متابعة الكروت، وفحص اشتباه مشاركة الكروت، وتوليد كروت جديدة مباشرة من خلال الأزرار التفاعلية أدناه:
        """.trimIndent()

        sendKeyboard(token, chatId, welcomeText, keyboard)
    }

    suspend fun sendCardsSubMenu(
        token: String,
        chatId: String,
        customMessage: String? = null
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val keyboard = JSONArray().apply {
            // Row 1
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🔍 البحث عن كرت") })
                put(JSONObject().apply { put("text", "📋 الكروت المتاحة") })
            })
            // Row 2
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "➕ إنشاء كروت") })
                put(JSONObject().apply { put("text", "📊 استهلاك كرت") })
            })
            // Row 3
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🟢 تفعيل كرت") })
                put(JSONObject().apply { put("text", "🔴 ايقاف كرت") })
            })
            // Row 4
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🗑️ حذف كرت") })
                put(JSONObject().apply { put("text", "⚠️ فحص الاشتباه") })
            })
            // Row 5: Return to Main
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🔙 القائمة الرئيسية") })
            })
        }

        val text = customMessage ?: """
            🎫 <b>قسم إدارة ومراقبة كروت الشبكة:</b>
            ━━━━━━━━━━━━━━━━━━
            اختر الإجراء المطلوب من الأزرار التفاعلية:
            • <b>البحث عن كرت:</b> الاستعلام عن بيانات وصلاحية واستهلاك كرت معين.
            • <b>إنشاء كروت:</b> توليد كروت هوتسبوت أو يوزرمانجر حسب الفئة والعدد.
            • <b>تفعيل / إيقاف كرت:</b> التحكم بحالة الكرت فوراً.
            • <b>استهلاك كرت:</b> معرفة حجم التحميل والرفع وتاريخ أول تسجيل دخول.
        """.trimIndent()

        sendKeyboard(token, chatId, text, keyboard)
    }

    suspend fun sendVoucherSystemOptions(
        token: String,
        chatId: String
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val keyboard = JSONArray().apply {
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🎫 يوزرمانجر") })
                put(JSONObject().apply { put("text", "🎫 هوتسبوت") })
            })
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🔙 القائمة الرئيسية") })
            })
        }

        val text = """
            🎫 <b>إنشاء كروت جديدة</b>
            ━━━━━━━━━━━━━━━━━━
            اختر النظام المطلوب لإنشاء الكروت:
            ▫️ <b>يوزرمانجر (User Manager)</b>
            ▫️ <b>هوتسبوت (Hotspot Users)</b>
        """.trimIndent()

        sendKeyboard(token, chatId, text, keyboard)
    }

    suspend fun sendProfilesPicker(
        token: String,
        chatId: String,
        systemName: String,
        profiles: List<String>
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val keyboard = JSONArray().apply {
            profiles.chunked(2).forEach { chunk ->
                put(JSONArray().apply {
                    chunk.forEach { prof ->
                        put(JSONObject().apply { put("text", "📦 $prof") })
                    }
                })
            }
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "❌ إلغاء") })
            })
        }

        val text = """
            اختر الـ <b>Profile</b> (لن يتم إنشاء أو تعديل أي Profile تلقائيًا - فقط تستخدم الموجود فعليًا):
            ━━━━━━━━━━━━━━━━━━
            النظام المحدد: <b>$systemName</b>
            اضغط على اسم الباقة أو الفئة المطلوبة:
        """.trimIndent()

        sendKeyboard(token, chatId, text, keyboard)
    }

    suspend fun sendValidityPicker(
        token: String,
        chatId: String,
        selectedProfile: String,
        selectedCount: Int
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val keyboard = JSONArray().apply {
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "بدون تحديد") })
                put(JSONObject().apply { put("text", "❌ إلغاء") })
            })
        }

        val text = """
            📅 <b>تحديد المدة</b>
            ━━━━━━━━━━━━━━━━━━
            الباقة: <b>$selectedProfile</b>
            العدد المطلوب: <b>$selectedCount كرت</b>
            
            هل تريد تحديد عدد الأيام لصلاحية الكرت؟
            ابعت رقم الأيام (مثلاً 7 أو 30)، أو دوس "بدون تحديد" للتخطي (بدون حد للمدة).
        """.trimIndent()

        sendKeyboard(token, chatId, text, keyboard)
    }

    suspend fun sendSuspiciousAlert(
        token: String,
        chatId: String,
        alert: SuspiciousCardSharingAlert
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val macsFormatted = alert.macAddresses.joinToString("\n") { "• $it" }
        val message = """
            ⚠️ <b>اشتباه مشاركة كارت</b>
            
            الكارت: <code>${alert.cardUsername}</code>
            ظهر على ${alert.macAddresses.size} أجهزة خلال آخر 24 ساعة:
            $macsFormatted
            
            تقدر توقفه من التطبيق (تحليلات الاستهلاك) أو من البوت عبر الأمر:
            <code>/disable_card_${alert.cardUsername}</code>
        """.trimIndent()

        sendMessage(token, chatId, message, parseMode = "HTML")
    }

    suspend fun deleteWebhookAndReset(token: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return@withContext Pair(true, null)
        try {
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/deleteWebhook?drop_pending_updates=true")
                .get()
                .build()
            val resp = client.newCall(req).execute()
            Pair(resp.isSuccessful, null)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage)
        }
    }
}
