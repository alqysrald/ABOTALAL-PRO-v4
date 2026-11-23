package com.example.ui.utils

import com.example.data.model.LogSeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ArabicLogTranslator {

    private val topicArabicMap = mapOf(
        "critical" to "حرج",
        "error" to "خطأ",
        "warning" to "تحذير",
        "info" to "معلومات",
        "system" to "النظام",
        "account" to "الحسابات",
        "hotspot" to "الهوتسبوت",
        "wireless" to "الوايرلس",
        "dhcp" to "DHCP",
        "ppp" to "PPPoE / PPP",
        "pppoe" to "PPPoE",
        "l2tp" to "L2TP",
        "sstp" to "SSTP",
        "ovpn" to "OpenVPN",
        "ipsec" to "IPsec",
        "firewall" to "جدار الحماية",
        "interface" to "المنافذ",
        "ethernet" to "الإيثرنت",
        "bridge" to "الجسر Bridge",
        "vlan" to "فيلان VLAN",
        "script" to "السكربتات",
        "backup" to "النسخ الاحتياطي",
        "cloud" to "الكلاود",
        "dns" to "DNS",
        "ntp" to "الوقت NTP",
        "clock" to "الساعة",
        "route" to "التوجيه",
        "bgp" to "BGP",
        "ospf" to "OSPF",
        "radius" to "الراديوس",
        "manager" to "يوزرمانجر",
        "user-manager" to "يوزرمانجر",
        "usermanager" to "يوزرمانجر",
        "fetch" to "جلب البيانات",
        "e-mail" to "البريد",
        "caps" to "CAPsMAN",
        "ups" to "الطاقة UPS",
        "health" to "حرارة/فولتية",
        "watchdog" to "المراقب Watchdog",
        "debug" to "تشخيص",
        "state" to "الحالة",
        "event" to "حدث"
    )

    fun translateTopics(rawTopics: String): String {
        if (rawTopics.isBlank()) return "النظام"
        val parts = rawTopics.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
        val translated = parts
            .filterNot { it == "debug" || it == "raw" }
            .mapNotNull { topicArabicMap[it] ?: it.uppercase(Locale.ROOT) }
            .distinct()
        return if (translated.isEmpty()) "النظام" else translated.joinToString(" • ")
    }

    fun detectSeverity(rawTopics: String, message: String): LogSeverity {
        val t = rawTopics.lowercase(Locale.ROOT)
        val m = message.lowercase(Locale.ROOT)
        return when {
            t.contains("critical") ||
                m.contains("out of memory") ||
                m.contains("without proper shutdown") ||
                m.contains("loop detected") ||
                m.contains("kernel failure") ||
                m.contains("fatal") ||
                m.contains("overheat") -> LogSeverity.CRITICAL

            t.contains("error") ||
                m.contains("failed") ||
                m.contains("failure") ||
                m.contains("error") ||
                m.contains("invalid") ||
                m.contains("denied") ||
                m.contains("rejected") ||
                m.contains("cannot") ||
                m.contains("unreachable") ||
                m.contains("link down") ||
                m.contains("disconnected") -> LogSeverity.ERROR

            t.contains("warning") ||
                m.contains("warn") ||
                m.contains("conflict") ||
                m.contains("timeout") ||
                m.contains("timed out") ||
                m.contains("already active") ||
                m.contains("simultaneous") ||
                m.contains("no more sessions") ||
                m.contains("not responding") ||
                m.contains("retry") ||
                m.contains("high cpu") ||
                m.contains("without success") -> LogSeverity.WARNING

            else -> LogSeverity.INFO
        }
    }

    fun translateMessage(rawTopics: String, rawMessage: String): String {
        val msg = rawMessage.trim()
        if (msg.isBlank()) return "رسالة نظام فارغة"
        // If already mostly Arabic, return as-is
        if (msg.any { it in '\u0600'..'\u06FF' } && msg.count { it in 'a'..'z' || it in 'A'..'Z' } < 8) {
            return msg
        }

        val lower = msg.lowercase(Locale.ROOT)

        // 1. Admin/Router login & logout
        Regex("""user\s+(\S+)\s+logged\s+in\s+from\s+(\S+)\s+via\s+(\S+)""", RegexOption.IGNORE_CASE)
            .find(msg)?.let { m ->
                return "تم تسجيل دخول المسؤول [${m.groupValues[1]}] من العنوان [${m.groupValues[2]}] عبر [${m.groupValues[3]}]"
            }
        Regex("""user\s+(\S+)\s+logged\s+out\s+from\s+(\S+)\s+via\s+(\S+)""", RegexOption.IGNORE_CASE)
            .find(msg)?.let { m ->
                return "تم تسجيل خروج المسؤول [${m.groupValues[1]}] من العنوان [${m.groupValues[2]}] عبر [${m.groupValues[3]}]"
            }
        Regex("""login\s+failure\s+for\s+user\s+(\S+)\s+from\s+(\S+)\s+via\s+(\S+)""", RegexOption.IGNORE_CASE)
            .find(msg)?.let { m ->
                return "فشل تسجيل الدخول للمستخدم [${m.groupValues[1]}] من العنوان [${m.groupValues[2]}] عبر [${m.groupValues[3]}] — يرجى التحقق من كلمة المرور أو الانتباه لمحاولة دخول غير مصرح بها"
            }

        // 2. Hotspot user login/logout/failures
        Regex("""(\S+)\s+\(([^)]+)\):\s+logged\s+in""", RegexOption.IGNORE_CASE)
            .find(msg)?.let { m ->
                return "تسجيل دخول كرت هوتسبوت [${m.groupValues[1]}] بالعنوان [${m.groupValues[2]}]"
            }
        Regex("""(\S+)\s+\(([^)]+)\):\s+logged\s+out:\s*(.+)""", RegexOption.IGNORE_CASE)
            .find(msg)?.let { m ->
                val reason = translateReason(m.groupValues[3])
                return "خروج كرت هوتسبوت [${m.groupValues[1]}] (${m.groupValues[2]}) — السبب: $reason"
            }
        Regex("""(\S+)\s+\(([^)]+)\):\s+login\s+failed:\s*(.+)""", RegexOption.IGNORE_CASE)
            .find(msg)?.let { m ->
                val reason = translateReason(m.groupValues[3])
                return "فشل دخول كرت هوتسبوت [${m.groupValues[1]}] (${m.groupValues[2]}) — السبب: $reason"
            }
        if (lower.contains("simultaneous session limit reached") || lower.contains("no more sessions are allowed")) {
            return "تم رفض الاتصال: هذا الكرت وصل للحد الأقصى للأجهزة المسموح بتشغيلها في نفس الوقت"
        }
        if (lower.contains("invalid username or password")) {
            return "فشل التحقق: اسم المستخدم أو كلمة المرور غير صحيحة"
        }
        if (lower.contains("user") && lower.contains("already active")) {
            return "تنبيه: الكرت متصل ونشط حالياً على جهاز آخر ($msg)"
        }
        if (lower.contains("traffic limit reached") || lower.contains("no valid profile found")) {
            return "انتهاء رصيد الكرت أو عدم وجود باقة صالحة لهذا المستخدم"
        }

        // 3. Interface Link Up / Link Down / Loop
        Regex("""(\S+)\s+link\s+down""", RegexOption.IGNORE_CASE).find(msg)?.let { m ->
            return "انقطاع الاتصال بالمنفذ [${m.groupValues[1]}] (Link Down) — تحقق من الكيبل أو جهاز التوزيع المتصل"
        }
        Regex("""(\S+)\s+link\s+up\s*(.*)""", RegexOption.IGNORE_CASE).find(msg)?.let { m ->
            val extra = m.groupValues[2].trim()
            return if (extra.isNotBlank()) {
                "عودة اتصال المنفذ [${m.groupValues[1]}] بنجاح ($extra)"
            } else {
                "عودة اتصال المنفذ [${m.groupValues[1]}] بنجاح (Link Up)"
            }
        }
        if (lower.contains("loop detected")) {
            return "خطر حرج: تم اكتشاف التماس أو حلقة شبكية (Loop) في الشبكة! ($msg)"
        }

        // 4. DHCP Leases & Conflicts
        Regex("""\S+\s+assigned\s+(\S+)\s+(?:for|to)\s+(\S+)""", RegexOption.IGNORE_CASE).find(msg)?.let { m ->
            return "توزيع عنوان IP [${m.groupValues[1]}] للجهاز [${m.groupValues[2]}] عبر DHCP"
        }
        Regex("""\S+\s+deassigned\s+(\S+)\s+from\s+(\S+)""", RegexOption.IGNORE_CASE).find(msg)?.let { m ->
            return "تحرير وإلغاء عنوان IP [${m.groupValues[1]}] من الجهاز [${m.groupValues[2]}]"
        }
        Regex("""offering\s+lease\s+(\S+)\s+for\s+(\S+)\s+without\s+success""", RegexOption.IGNORE_CASE).find(msg)?.let { m ->
            return "تحذير DHCP: تعذر إتمام منح العنوان [${m.groupValues[1]}] للجهاز [${m.groupValues[2]}] (قد يكون هناك تعارض أو ضعف إشارة)"
        }
        if (lower.contains("conflict") || lower.contains("duplicate")) {
            return "تحذير: وجود تعارض عناوين IP أو MAC متكرر على الشبكة ($msg)"
        }

        // 5. PPP / PPPoE / VPN
        if (lower.contains("peer is not responding")) {
            return "انقطاع اتصال العميل/النفق (PPP): الطرف الآخر لا يستجيب ($msg)"
        }
        if (lower.contains("terminating") && lower.contains("hungup")) {
            return "إنهاء جلسة اتصال PPP بسبب انقطاع الخط ($msg)"
        }

        // 6. System, Power, Memory, Scripts, Config Changes
        if (lower.contains("without proper shutdown")) {
            return "تحذير حرج: أعيد تشغيل الراوتر بشكل مفاجئ بدون إيقاف سليم (انقطاع كهرباء أو إعادة تشغيل قسرية)"
        }
        if (lower.contains("out of memory") || lower.contains("low memory")) {
            return "خطأ حرج: ذاكرة الراوتر العشوائية (RAM) ممتلئة أو منخفضة جداً"
        }
        if (lower.contains("script error") || (rawTopics.contains("script", true) && lower.contains("error"))) {
            return "خطأ في تنفيذ سكربت داخل المايكروتك: ${translateCommonWords(msg)}"
        }
        if (lower.contains("system backup saved") || lower.contains("backup created")) {
            return "تم حفظ نسخة احتياطية كاملة لإعدادات الراوتر بنجاح"
        }
        if (lower.contains("cloud") && lower.contains("updated")) {
            return "تم تحديث عنوان الكلاود (IP Cloud DDNS) ومزامنة IP الراوتر العام بنجاح"
        }
        if (lower.contains("cloud") && (lower.contains("failed") || lower.contains("error"))) {
            return "فشل تحديث خدمة الكلاود (IP Cloud): تحقق من اتصال الراوتر بالإنترنت"
        }
        Regex("""(.+?)\s+(added|changed|removed)\s+by\s+(\S+)""", RegexOption.IGNORE_CASE).find(msg)?.let { m ->
            val verb = when (m.groupValues[2].lowercase(Locale.ROOT)) {
                "added" -> "تمت إضافة"
                "changed" -> "تم تعديل"
                "removed" -> "تم حذف"
                else -> "تم تحديث"
            }
            return "$verb إعداد [${m.groupValues[1].trim()}] بواسطة المستخدم [${m.groupValues[3]}]"
        }

        // 7. Generic intelligent Arabic translation fallback
        val topicLabel = translateTopics(rawTopics).split(" • ").firstOrNull() ?: "النظام"
        return "[$topicLabel] ${translateCommonWords(msg)}"
    }

    private fun translateReason(reason: String): String {
        val r = reason.trim().lowercase(Locale.ROOT)
        return when {
            r.contains("session timeout") -> "انتهاء صلاحية الوقت المحدد للكرت"
            r.contains("idle timeout") -> "خمول الجهاز وعدم وجود سحب بيانات"
            r.contains("keepalive timeout") -> "انقطاع اتصال الجهاز بالشبكة (Keepalive)"
            r.contains("traffic limit") || r.contains("bytes") -> "نفاد رصيد البيانات المسموح به"
            r.contains("admin reset") -> "تم الفصل يدوياً بواسطة المدير"
            r.contains("user request") -> "طلب تسجيل خروج من المستخدم"
            r.contains("nas") -> "إعادة ضبط من الراوتر"
            r.contains("simultaneous") -> "تجاوز عدد الأجهزة المسموحة لنفس الكرت"
            r.contains("invalid") -> "بيانات الكرت غير صحيحة"
            else -> translateCommonWords(reason)
        }
    }

    fun translateCommonWords(input: String): String {
        var out = input
        val replacements = listOf(
            "connection refused" to "تم رفض الاتصال",
            "connection timed out" to "انتهت مهلة الاتصال",
            "timed out" to "انتهت المهلة",
            "timeout" to "انتهاء المهلة",
            "authentication failed" to "فشل المصادقة",
            "login failed" to "فشل تسجيل الدخول",
            "failed" to "فشل",
            "failure" to "فشل",
            "error" to "خطأ",
            "warning" to "تحذير",
            "critical" to "حرج",
            "disconnected" to "انقطع الاتصال",
            "connected" to "متصل",
            "unreachable" to "غير قابل للوصول",
            "denied" to "مرفوض",
            "rejected" to "تم الرفض",
            "disabled" to "معطل",
            "enabled" to "مفعل",
            "not found" to "غير موجود",
            "already exists" to "موجود مسبقاً",
            "started" to "بدأ التشغيل",
            "stopped" to "توقف",
            "updated" to "تم التحديث",
            "added" to "تمت الإضافة",
            "removed" to "تم الحذف",
            "changed" to "تم التعديل",
            "interface" to "المنفذ",
            "bridge" to "الجسر",
            "address" to "العنوان",
            "port" to "المنفذ"
        )
        for ((eng, ar) in replacements) {
            out = out.replace(Regex("\\b$eng\\b", RegexOption.IGNORE_CASE), ar)
        }
        return out
    }

    fun translateAppErrorToArabic(rawText: String): String {
        val text = rawText.trim()
        if (text.isBlank()) return "لا توجد تفاصيل إضافية"
        // If already predominantly Arabic
        if (text.any { it in '\u0600'..'\u06FF' } && !text.contains("Exception", true)) {
            return text
        }
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("connectexception") || lower.contains("failed to connect") || lower.contains("econnrefused") ->
                "تعذر الاتصال بالراوتر: المنفذ مغلق أو خدمة API غير مفعلة في IP > Services"
            lower.contains("sockettimeoutexception") || lower.contains("timed out") ->
                "انتهت مهلة الاتصال بالراوتر — تحقق من وصول الإنترنت أو عنوان IP/Cloud"
            lower.contains("unknownhostexception") ->
                "تعذر العثور على عنوان الراوتر أو اسم الدومين (تحقق من DNS أو الاتصال)"
            lower.contains("cannot log in") || lower.contains("invalid user name or password") ->
                "اسم المستخدم أو كلمة المرور للراوتر غير صحيحة"
            else -> translateCommonWords(text)
        }
    }

    fun formatTimestamp(ts: Long): String {
        return try {
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale("ar")).format(Date(ts))
        } catch (_: Exception) {
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(Date(ts))
        }
    }
}
