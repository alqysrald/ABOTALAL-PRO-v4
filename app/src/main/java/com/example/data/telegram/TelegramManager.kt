package com.example.data.telegram

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TelegramBotInfo(
    val isSuccess: Boolean,
    val botId: Long = 0L,
    val botName: String = "",
    val username: String = "",
    val errorMessage: String? = null
)

data class TelegramChatInfo(
    val chatId: String,
    val titleOrUsername: String,
    val type: String
)

data class TelegramIncomingUpdate(
    val updateId: Long,
    val chatId: String,
    val text: String,
    val senderName: String
)

class TelegramManager {
    private val tag = "TelegramManager"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testBotToken(token: String): TelegramBotInfo = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext TelegramBotInfo(isSuccess = false, errorMessage = "رمز البوت (Token) فارغ")
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
                    return@withContext TelegramBotInfo(
                        isSuccess = true,
                        botId = result.optLong("id"),
                        botName = result.optString("first_name"),
                        username = result.optString("username")
                    )
                } else {
                    val desc = json.optString("description", "فشل التحقق من البوت")
                    return@withContext TelegramBotInfo(isSuccess = false, errorMessage = desc)
                }
            } else {
                return@withContext TelegramBotInfo(isSuccess = false, errorMessage = "رمز البوت غير صالح (401 Unauthorized)")
            }
        } catch (e: Exception) {
            Log.e(tag, "Telegram test error", e)
            return@withContext TelegramBotInfo(isSuccess = false, errorMessage = "تعذر الاتصال بسيرفر تليجرام: ${e.localizedMessage}")
        }
    }

    suspend fun getRecentChats(token: String): List<TelegramChatInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TelegramChatInfo>()
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return@withContext list

        try {
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/getUpdates")
                .get()
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                if (json.optBoolean("ok", false)) {
                    val results = json.getJSONArray("result")
                    val seenIds = mutableSetOf<String>()
                    for (i in 0 until results.length()) {
                        val update = results.getJSONObject(i)
                        val msg = update.optJSONObject("message")
                            ?: update.optJSONObject("channel_post")
                            ?: update.optJSONObject("my_chat_member")
                        val chat = msg?.optJSONObject("chat")
                        if (chat != null) {
                            val id = chat.opt("id").toString()
                            if (!seenIds.contains(id)) {
                                seenIds.add(id)
                                val title = chat.optString("title").ifBlank {
                                    chat.optString("username").ifBlank {
                                        "${chat.optString("first_name")} ${chat.optString("last_name")}".trim()
                                    }
                                }
                                val type = chat.optString("type", "private")
                                list.add(TelegramChatInfo(chatId = id, titleOrUsername = title, type = type))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get updates", e)
        }
        list
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

    suspend fun sendMessage(token: String, chatId: String, message: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val cleanChat = chatId.trim()
        if (cleanToken.isBlank() || cleanChat.isBlank()) {
            return@withContext Pair(false, "يجب إدخال التوكن ومعرف المحادثة Chat ID")
        }
        try {
            val json = JSONObject().apply {
                put("chat_id", cleanChat)
                put("text", message)
                put("parse_mode", "HTML")
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
                    val desc = resJson.optString("description", "فشل الإرسال")
                    return@withContext Pair(false, desc)
                }
            } else {
                return@withContext Pair(false, "فشل الإرسال (رمز الاستجابة: ${resp.code})")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send telegram msg", e)
            return@withContext Pair(false, e.localizedMessage ?: "خطأ في الشبكة")
        }
    }

    /**
     * Sends the complete Arabic interactive keypad buttons requested by the user:
     * - زر القائمة
     * - زر الحالة
     * - زر المتصلين
     * - زر الباقات
     * - زر نسخة احتياطية
     * - زر حماية المواقع
     * - زر تحسين الشبكة
     * - زر التقرير اليومي
     * - زر المبيعات
     * - زر ملخص الاداء
     * - زر الاستهلاك اليومي
     */
    suspend fun sendInteractiveKeyboard(
        token: String,
        chatId: String,
        routerName: String,
        customMessage: String? = null
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val cleanChat = chatId.trim()
        if (cleanToken.isBlank() || cleanChat.isBlank()) {
            return@withContext Pair(false, "بيانات التوكن أو المحادثة غير مكتملة")
        }

        try {
            // Build the Arabic reply keyboard
            val keyboardArray = JSONArray().apply {
                // Row 1
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "📋 القائمة") })
                    put(JSONObject().apply { put("text", "🟢 الحالة") })
                })
                // Row 2
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "👥 المتصلين") })
                    put(JSONObject().apply { put("text", "📦 الباقات") })
                })
                // Row 3
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "💾 نسخة احتياطية") })
                    put(JSONObject().apply { put("text", "🛡️ حماية المواقع") })
                })
                // Row 4
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "⚡ تحسين الشبكة") })
                    put(JSONObject().apply { put("text", "📊 التقرير اليومي") })
                })
                // Row 5
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "💰 المبيعات") })
                    put(JSONObject().apply { put("text", "📈 ملخص الاداء") })
                })
                // Row 6
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "🌐 الاستهلاك اليومي") })
                })
                // Row 7 (New: Distribution Units & Ports)
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "📡 قطع التوزيع") })
                    put(JSONObject().apply { put("text", "🔌 منافذ الشبكة") })
                })
                // Row 8 (New: Port Consumption & Controls)
                put(JSONArray().apply {
                    put(JSONObject().apply { put("text", "📊 استهلاك المنافذ") })
                    put(JSONObject().apply { put("text", "⚙️ تحكم المنافذ") })
                })
            }

            val replyMarkup = JSONObject().apply {
                put("keyboard", keyboardArray)
                put("resize_keyboard", true)
                put("is_persistent", true)
            }

            val textToSend = customMessage ?: """
                📡 <b>لوحة تحكم TALAL PRO الذكية في تليجرام</b>
                ━━━━━━━━━━━━━━━━━━
                🔗 <b>الراوتر المتصل:</b> $routerName
                ⚡ <b>حالة الربط السحابي (Cloud):</b> نشط ومربوط مباشرة بالمايكروتك.
                
                اضغط على أي زر أدناه لتنفيذ الأوامر وعرض التقارير اللحظية بدون كتابة:
            """.trimIndent()

            val json = JSONObject().apply {
                put("chat_id", cleanChat)
                put("text", textToSend)
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
                    return@withContext Pair(false, resJson.optString("description", "فشل الإرسال"))
                }
            } else {
                return@withContext Pair(false, "فشل الإرسال (${resp.code})")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send keyboard", e)
            return@withContext Pair(false, e.localizedMessage)
        }
    }

    fun generateRouterOsScript(token: String, chatId: String, routerName: String = "TALAL_ROUTER"): String {
        val d = "$"
        return """
            # ========================================================
            # TALAL PRO - Telegram Notification Script for MikroTik
            # Generated automatically for Router: $routerName
            # ========================================================
            :local botToken "$token"
            :local chatId "$chatId"
            :local rName [/system identity get name]
            :local rTime [/system clock get time]
            :local rDate [/system clock get date]
            :local rCpu [/system resource get cpu-load]
            :local rUptime [/system resource get uptime]
            :local rFreeMem [/system resource get free-memory]

            :local message "🔔 *تنبيه من راوتر TALAL PRO*%0A------------------------%0A📡 *الراوتر:* ${d}rName%0A⏱ *الوقت:* ${d}rTime - ${d}rDate%0A⚡ *المعالج:* ${d}rCpu%%0A⏳ *مدة التشغيل:* ${d}rUptime%0A💾 *الذاكرة المتاحة:* ${d}rFreeMem%0A✅ حالة المنظومة: طبيعية ومستقرة."

            /tool fetch url="https://api.telegram.org/bot${d}botToken/sendMessage?chat_id=${d}chatId&text=${d}message&parse_mode=Markdown" keep-result=no
        """.trimIndent()
    }
}
