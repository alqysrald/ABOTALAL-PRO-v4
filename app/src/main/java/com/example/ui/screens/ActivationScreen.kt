package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseUserAccount
import com.example.data.utils.DeviceUtils
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

@Composable
fun ActivationScreen(
    viewModel: TalalProViewModel,
    user: FirebaseUserAccount?,
    onActivated: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputActivationCode by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val currentEmail = user?.email ?: ""
    val currentPhone = user?.phone ?: ""
    val currentName = user?.name ?: ""
    val networkName = user?.networkName ?: ""
    val deviceFingerprint = user?.deviceFingerprint.takeIf { !it.isNullOrBlank() } ?: DeviceUtils.getDeviceFingerprint(context)
    val licenseRequestCode = user?.licenseRequestCode.takeIf { !it.isNullOrBlank() } ?: DeviceUtils.generateLicenseRequestCode(context, currentEmail, currentPhone)
    val deviceModel = DeviceUtils.getDeviceModel()

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label بنجاح ✓", Toast.LENGTH_SHORT).show()
    }

    fun sendToWhatsApp() {
        val whatsappNumber = "967778215553"
        val message = """
            السلام عليكم ورحمة الله،
            طلب تفعيل منظومة TALAL PRO v3.0:
            ━━━━━━━━━━━━━━━━━━━━
            👤 الاسم الكامل: $currentName
            🌐 اسم الشبكة: $networkName
            📱 رقم الهاتف: $currentPhone
            📧 البريد الإلكتروني: $currentEmail
            📱 موديل الجهاز: $deviceModel
            🔑 بصمة الجهاز: $deviceFingerprint
            ━━━━━━━━━━━━━━━━━━━━
            🏷️ كود طلب الترخيص:
            $licenseRequestCode
            ━━━━━━━━━━━━━━━━━━━━
            يرجى تزويدي بكود التفعيل المعتمد. شكراً لكم.
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$whatsappNumber&text=${Uri.encode(message)}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Security Shield
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                        )
                    )
                    .shadow(12.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = "Security License",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "تفعيل ترخيص المنظومة",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TALAL PRO - حماية وترخيص أحادي للجهاز",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // User & Device Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Text(
                            text = "بيانات الحساب والجهاز المسجل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    InfoRow(label = "الاسم الكامل", value = currentName.ifBlank { "غير محدد" })
                    InfoRow(label = "اسم الشبكة", value = networkName.ifBlank { "الشبكة الرئيسية" })
                    InfoRow(label = "رقم الهاتف", value = currentPhone)
                    InfoRow(label = "البريد الإلكتروني", value = currentEmail)
                    InfoRow(label = "موديل الهاتف", value = deviceModel)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "بصمة الجهاز الفريدة (Fingerprint):", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(text = deviceFingerprint, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF38BDF8))
                        }
                        IconButton(onClick = { copyToClipboard("بصمة الجهاز", deviceFingerprint) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // BOX 1: License Request Code & WhatsApp
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "الخانة الأولى: كود طلب الترخيص المولد",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "هذا الكود مشفر وخاص بهاتفك وحسابك، قم بنسخه أو إرساله مباشرة إلى الإدارة لتوليد كود التفعيل:",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 18.sp
                    )

                    // Request Code Display Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = licenseRequestCode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = { copyToClipboard("كود طلب الترخيص", licenseRequestCode) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الكود", tint = Color(0xFF38BDF8))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { copyToClipboard("كود طلب الترخيص", licenseRequestCode) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ الكود", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { sendToWhatsApp() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            modifier = Modifier.weight(1.3f).height(46.dp).testTag("send_whatsapp_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال للواتساب", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Text(
                        text = "رقم واتساب الإدارة المعتمد: 778215553",
                        fontSize = 11.sp,
                        color = Color(0xFF86EFAC),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // BOX 2: Input Activation Code
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEAB308)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "الخانة الثانية: إدخال كود التفعيل الممنوح",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "أدخل كود التفعيل المستلم من الإدارة لتنشيط حسابك وجهازك بشكل نهائي:",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )

                    OutlinedTextField(
                        value = inputActivationCode,
                        onValueChange = {
                            inputActivationCode = it.uppercase()
                            errorMessage = null
                        },
                        placeholder = { Text("مثال: ACT-XXXX-XXXX-XXXX-XXXX", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFEAB308))
                        },
                        trailingIcon = {
                            if (inputActivationCode.isNotBlank()) {
                                IconButton(onClick = { inputActivationCode = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFFEAB308),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_code_input")
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { err ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF7F1D1D),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                                    Text(text = err, color = Color(0xFFFEE2E2), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (inputActivationCode.isBlank()) {
                                errorMessage = "يرجى إدخال كود التفعيل المستلم من الإدارة"
                                return@Button
                            }
                            isSubmitting = true
                            errorMessage = null
                            viewModel.activateUserLicense(currentEmail, inputActivationCode) { success, err ->
                                isSubmitting = false
                                if (success) {
                                    showSuccessDialog = true
                                } else {
                                    errorMessage = err ?: "كود التفعيل غير صالح"
                                }
                            }
                        },
                        enabled = !isSubmitting && inputActivationCode.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_activation_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color(0xFF0F172A), strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري التحقق من كود الترخيص...", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تفعيل المنظومة الآن ✓", color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Important Security Instructions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "شروط وسياسة الترخيص الأمنية:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "• التطبيق يرتبط حصرياً ببصمة هذا الهاتف ولا يمكن نقله لجهاز آخر إلا بموافقة الإدارة.\n• لن يتم تكرار تسجيل الدخول بعد التفعيل وسيعمل التطبيق مباشرة.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Switch account / Logout button
            TextButton(
                onClick = onLogout,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تسجيل الخروج أو استخدام حساب آخر", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
    }

    // Success Activation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            },
            title = {
                Text(
                    text = "تم تفعيل الترخيص بنجاح! 🎉",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تم تفعيل منظومة TALAL PRO وربطها بنجاح مع هذا الجهاز ($deviceModel).",
                        fontSize = 13.sp,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "يمكنك الآن إدارة ومراقبة راوترات وشبكات المايكروتيك بكامل الصلاحيات.",
                        fontSize = 12.sp,
                        color = TextSecondaryLight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onActivated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("الدخول إلى لوحة التحكم الرئيسية", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
