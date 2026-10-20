package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "scale"
    )

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1800) // 1.8 seconds splash
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0F1D),
                        Color(0xFF0F172A),
                        Color(0xFF021B36),
                        Color(0xFF0A0F1D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
                .padding(24.dp)
        ) {
            // Glowing circular logo container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF38BDF8),
                                Color(0xFF0284C7),
                                Color(0xFF0369A1)
                            )
                        )
                    )
                    .shadow(24.dp, CircleShape, spotColor = Color(0xFF38BDF8))
            ) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = "TALAL PRO Logo",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Brand Name
            Text(
                text = "TALAL PRO",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0369A1).copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            ) {
                Text(
                    text = "الإصدار الثالث V3.0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "المنظومة الاحترافية الشاملة لإدارة وتحكم شبكات المايكروتيك",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
        }

        // Bottom Developer Credits
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                Text(
                    text = "إعداد وتطوير: عبدالحميد داوؤد",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "هاتف الدعم: 778215553 - 700897775",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
