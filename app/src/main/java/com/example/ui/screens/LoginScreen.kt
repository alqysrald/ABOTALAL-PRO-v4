package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

enum class AuthTab {
    LOGIN,
    REGISTER
}

@Composable
fun LoginScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AuthTab.LOGIN) }

    // Login Form State
    var loginEmailOrUser by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register Form State
    var regFullName by remember { mutableStateOf("") }
    var regGovernorate by remember { mutableStateOf("") }
    var regCity by remember { mutableStateOf("") }
    var regNetworkName by remember { mutableStateOf("") }
    var regNetworkLocation by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    val isCheckingAuth by viewModel.isCheckingAuth.collectAsState()
    val authErrorMsg by viewModel.authError.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchCurrentLocation(context) { locStr ->
                regNetworkLocation = locStr
                isLocating = false
                Toast.makeText(context, "تم تحديد موقع الشبكة بنجاح ✓", Toast.LENGTH_SHORT).show()
            }
        } else {
            isLocating = false
            Toast.makeText(context, "تم رفض إذن الموقع، يمكنك كتابة الموقع يدوياً", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocation() {
        isLocating = true
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchCurrentLocation(context) { locStr ->
                regNetworkLocation = locStr
                isLocating = false
                Toast.makeText(context, "تم تحديد موقع الشبكة بنجاح ✓", Toast.LENGTH_SHORT).show()
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0),
                        Color(0xFFF8FAFC)
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
            Spacer(modifier = Modifier.height(8.dp))

            // App Brand Banner
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryNavy, PrimaryBlue)
                        )
                    )
                    .shadow(10.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = "TALAL PRO",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TALAL PRO",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryNavy,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "منظومة إدارة وحماية شبكات المايكروتيك الاحترافية",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryLight
                )
            }

            if (appConfig.allowGuestAccess) {
                Button(
                    onClick = { viewModel.enterGuestMode(initialSilent = false) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("guest_mode_enter_button")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الدخول المباشر للتطبيق بدون تسجيل",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Top Two Professional Navigation Tabs
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Login Tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentTab == AuthTab.LOGIN) Brush.linearGradient(listOf(PrimaryNavy, PrimaryBlue))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable {
                                currentTab = AuthTab.LOGIN
                                viewModel.clearAuthError()
                            }
                            .testTag("tab_login")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = if (currentTab == AuthTab.LOGIN) Color.White else TextSecondaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تسجيل الدخول",
                                fontSize = 14.sp,
                                fontWeight = if (currentTab == AuthTab.LOGIN) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == AuthTab.LOGIN) Color.White else TextSecondaryLight
                            )
                        }
                    }

                    // Register Tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentTab == AuthTab.REGISTER) Brush.linearGradient(listOf(PrimaryNavy, PrimaryBlue))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable {
                                currentTab = AuthTab.REGISTER
                                viewModel.clearAuthError()
                            }
                            .testTag("tab_register")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (currentTab == AuthTab.REGISTER) Color.White else TextSecondaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تسجيل حساب جديد",
                                fontSize = 14.sp,
                                fontWeight = if (currentTab == AuthTab.REGISTER) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == AuthTab.REGISTER) Color.White else TextSecondaryLight
                            )
                        }
                    }
                }
            }

            // Form Container Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Global Error Message Banner
                    AnimatedVisibility(visible = authErrorMsg != null) {
                        authErrorMsg?.let { err ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = err,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB91C1C),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (currentTab == AuthTab.LOGIN) {
                        // ================= LOGIN FORM =================
                        Text(
                            text = "أهلاً بك مجدداً، يرجى إدخال بيانات حسابك:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryLight
                        )

                        // Email / Username Input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "البريد الإلكتروني / اسم الحساب / رقم الهاتف",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = loginEmailOrUser,
                                onValueChange = { loginEmailOrUser = it },
                                placeholder = { Text("أدخل البريد أو الهاتف المسجل", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input")
                            )
                        }

                        // Password Input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "كلمة المرور",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                placeholder = { Text("••••••••", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                                },
                                singleLine = true,
                                visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "إظهار كلمة المرور",
                                            tint = TextMutedLight
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input")
                            )
                        }

                        // Login Button
                        Button(
                            onClick = {
                                viewModel.login(loginEmailOrUser, loginPassword)
                            },
                            enabled = !isCheckingAuth && loginEmailOrUser.isNotBlank() && loginPassword.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button")
                        ) {
                            if (isCheckingAuth) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التحقق والمصادقة...", fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // ================= REGISTRATION FORM =================
                        Text(
                            text = "إنشاء حساب جديد وتوليد ترخيص الجهاز:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )

                        // 1. Full Name (الإسم الكامل)
                        FormField(
                            label = "الإسم الكامل *",
                            value = regFullName,
                            onValueChange = { regFullName = it },
                            placeholder = "مثال: عبدالحميد داوؤد",
                            icon = Icons.Default.Person
                        )

                        // 2. Governorate & City (المحافظة والمدينة)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = "المحافظة *",
                                    value = regGovernorate,
                                    onValueChange = { regGovernorate = it },
                                    placeholder = "مثال: صنعاء / عدن",
                                    icon = Icons.Default.LocationCity
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = "المدينة / المديرية *",
                                    value = regCity,
                                    onValueChange = { regCity = it },
                                    placeholder = "مثال: التحرير",
                                    icon = Icons.Default.Apartment
                                )
                            }
                        }

                        // 3. Network Name (اسم الشبكة)
                        FormField(
                            label = "اسم الشبكة *",
                            value = regNetworkName,
                            onValueChange = { regNetworkName = it },
                            placeholder = "مثال: شبكة طلال نت",
                            icon = Icons.Default.Wifi
                        )

                        // 4. Network Location (موقع الشبكة.. اختياري مع زر تحديد موقع الشبكة على الخريطة وشغال)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "موقع الشبكة (اختياري)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryLight
                                )
                                TextButton(
                                    onClick = { requestLocation() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    if (isLocating) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("جاري التحديد...", fontSize = 11.sp, color = PrimaryBlue)
                                    } else {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تحديد موقعي الآن 📍", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = regNetworkLocation,
                                onValueChange = { regNetworkLocation = it },
                                placeholder = { Text("إحداثيات أو عنوان الشبكة الجغرافي", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryBlue)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 5. Phone Number (رقم الهاتف إلزامي)
                        FormField(
                            label = "رقم الهاتف (إلزامي) *",
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            placeholder = "مثال: 778215553",
                            icon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone
                        )

                        // 6. Email (البريد الإلكتروني إلزامي)
                        FormField(
                            label = "البريد الإلكتروني (إلزامي) *",
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            placeholder = "name@example.com",
                            icon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email
                        )

                        // 7. Password (كلمة المرور)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "كلمة المرور *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                placeholder = { Text("أدخل كلمة مرور قوية", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                                },
                                singleLine = true,
                                visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = TextMutedLight
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 8. Confirm Password (تأكيد كلمة المرور)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "تأكيد كلمة المرور *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it },
                                placeholder = { Text("أعد كتابة كلمة المرور", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.LockReset, contentDescription = null, tint = PrimaryBlue)
                                },
                                singleLine = true,
                                visualTransformation = if (regConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { regConfirmPasswordVisible = !regConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = TextMutedLight
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Register Submit Button
                        Button(
                            onClick = {
                                if (regFullName.isBlank() || regPhone.isBlank() || regEmail.isBlank() || regPassword.isBlank()) {
                                    viewModel.setAuthError("يرجى ملء جميع الحقول الإلزامية المطلوبة (*)")
                                    return@Button
                                }
                                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(regEmail.trim()).matches()) {
                                    viewModel.setAuthError("البريد الإلكتروني المدخل غير صالح! يرجى إدخال بريد صحيح.")
                                    return@Button
                                }
                                if (regPassword != regConfirmPassword) {
                                    viewModel.setAuthError("كلمتا المرور غير متطابقتين! يرجى التأكد وإعادة المحاولة.")
                                    return@Button
                                }
                                if (regPassword.length < 6) {
                                    viewModel.setAuthError("يجب أن تكون كلمة المرور 6 أحرف أو أرقام على الأقل.")
                                    return@Button
                                }

                                viewModel.registerNewUser(
                                    fullName = regFullName,
                                    governorate = regGovernorate,
                                    city = regCity,
                                    networkName = regNetworkName,
                                    networkLocation = regNetworkLocation,
                                    phone = regPhone,
                                    email = regEmail,
                                    password = regPassword
                                )
                            },
                            enabled = !isCheckingAuth,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_button")
                        ) {
                            if (isCheckingAuth) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري فحص وتوثيق الحساب...", fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.AppRegistration, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الحساب وتوليد الترخيص 🔑",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Luxury Reassurance & Marketing Highlights Card (Replaces technical cloud text)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "حماية فائقة وأمان عالي لمنظومتك",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                                Text(
                                    text = "ترخيص آمن مشفر مرتبط بجهازك لضمان أعلى مستويات الاستقرار والسرية.",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Developer & Support Credits Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "إعداد وتطوير المنظومة |",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                        Text(
                            text = "عبدالحميد داوؤد",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                    }
                    Text(
                        text = "هاتف الدعم الفني: 778215553 - 700897775",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryLight,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Suppress("DEPRECATION")
private fun fetchCurrentLocation(context: Context, onLocationResult: (String) -> Unit) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm != null) {
            val isGps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNet = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var bestLoc: Location? = null
            if (isGps) {
                bestLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (bestLoc == null && isNet) {
                bestLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (bestLoc != null) {
                val locStr = "%.5f, %.5f".format(bestLoc.latitude, bestLoc.longitude)
                onLocationResult(locStr)
                return
            }
        }
        // Fallback default coordinates if not yet acquired
        onLocationResult("15.3694, 44.1910") // Default Sana'a coordinates
    } catch (e: Exception) {
        onLocationResult("15.3694, 44.1910")
    }
}
