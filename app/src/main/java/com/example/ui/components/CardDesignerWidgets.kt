package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CardTemplateEntity
import com.example.data.model.GeneratedCardPrintItem
import com.example.ui.theme.*
import com.example.ui.utils.CardPdfGenerator
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DesignerSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = PrimaryBlue,
    trailingSwitchChecked: Boolean? = null,
    onTrailingSwitchChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconTint.copy(alpha = 0.12f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryNavy
                    )
                }

                if (trailingSwitchChecked != null && onTrailingSwitchChange != null) {
                    Switch(
                        checked = trailingSwitchChecked,
                        onCheckedChange = onTrailingSwitchChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                    )
                }
            }

            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))

            content()
        }
    }
}

@Composable
fun DesignerToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryLight)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondaryLight)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
        )
    }
}

@Composable
fun IntStepperRow(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimaryLight,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalIconButton(
                onClick = { onValueChange((value - step).coerceIn(minValue, maxValue)) },
                enabled = value > minValue,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = PrimaryNavy
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(18.dp))
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .height(40.dp)
                    .clickable { showEditDialog = true }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        text = value.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryNavy,
                        textAlign = TextAlign.Center
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { onValueChange((value + step).coerceIn(minValue, maxValue)) },
                enabled = value < maxValue,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFFE0F2FE),
                    contentColor = PrimaryBlue
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showEditDialog) {
        var input by remember { mutableStateOf(value.toString()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = input.toIntOrNull()
                    if (parsed != null) {
                        onValueChange(parsed.coerceIn(minValue, maxValue))
                    }
                    showEditDialog = false
                }) {
                    Text("تطبيق")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun FloatStepperRow(
    label: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    step: Float = 0.5f,
    decimals: Int = 1,
    onValueChange: (Float) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    fun roundValue(v: Float): Float {
        val factor = Math.pow(10.0, decimals.toDouble()).toFloat()
        return (v * factor).roundToInt() / factor
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimaryLight,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalIconButton(
                onClick = { onValueChange(roundValue((value - step).coerceIn(minValue, maxValue))) },
                enabled = value > minValue + 0.001f,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = PrimaryNavy
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(18.dp))
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .height(40.dp)
                    .clickable { showEditDialog = true }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        text = String.format(Locale.US, "%.${decimals}f", value),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryNavy,
                        textAlign = TextAlign.Center
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { onValueChange(roundValue((value + step).coerceIn(minValue, maxValue))) },
                enabled = value < maxValue - 0.001f,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFFE0F2FE),
                    contentColor = PrimaryBlue
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showEditDialog) {
        var input by remember { mutableStateOf(String.format(Locale.US, "%.${decimals}f", value)) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = input.replace(",", ".").toFloatOrNull()
                    if (parsed != null) {
                        onValueChange(roundValue(parsed.coerceIn(minValue, maxValue)))
                    }
                    showEditDialog = false
                }) {
                    Text("تطبيق")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun DPadController(
    title: String = "تحريك العنصر داخل الكرت",
    xPercent: Float,
    yPercent: Float,
    stepPercent: Float = 3f,
    onMove: (newX: Float, newY: Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE0F2FE)) {
                Text(
                    text = "X: ${xPercent.toInt()}% • Y: ${yPercent.toInt()}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Up button
        FilledTonalIconButton(
            onClick = { onMove(xPercent, (yPercent - stepPercent).coerceIn(5f, 95f)) },
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White, contentColor = PrimaryNavy)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "أعلى")
        }

        // Left, Center Reset, Right
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { onMove((xPercent - stepPercent).coerceIn(5f, 95f), yPercent) },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White, contentColor = PrimaryNavy)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "يسار")
            }

            Surface(
                shape = CircleShape,
                color = PrimaryBlue.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onMove(50f, 50f) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MyLocation, contentDescription = "توسيط", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                }
            }

            FilledTonalIconButton(
                onClick = { onMove((xPercent + stepPercent).coerceIn(5f, 95f), yPercent) },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White, contentColor = PrimaryNavy)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "يمين")
            }
        }

        // Down button
        FilledTonalIconButton(
            onClick = { onMove(xPercent, (yPercent + stepPercent).coerceIn(5f, 95f)) },
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White, contentColor = PrimaryNavy)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "أسفل")
        }
    }
}

@Composable
fun ColorPickerFieldRow(
    label: String,
    colorHex: String,
    onColorSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val currentColor = remember(colorHex) {
        Color(CardPdfGenerator.parseColorSafe(colorHex, android.graphics.Color.BLACK))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimaryLight)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = colorHex.uppercase(),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(currentColor)
                    .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(6.dp))
            )
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            title = label,
            initialHex = colorHex,
            onDismiss = { showPicker = false },
            onConfirm = { newHex ->
                onColorSelected(newHex)
                showPicker = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(
    title: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val presetColors = listOf(
        "#000000", "#111827", "#1E293B", "#334155", "#64748B", "#FFFFFF",
        "#0284C7", "#1D4ED8", "#0F766E", "#16A34A", "#CA8A04", "#EA580C",
        "#DC2626", "#9333EA", "#DB2777", "#7C2D12"
    )
    val initialColorInt = CardPdfGenerator.parseColorSafe(initialHex, android.graphics.Color.BLACK)
    var red by remember { mutableFloatStateOf(android.graphics.Color.red(initialColorInt).toFloat()) }
    var green by remember { mutableFloatStateOf(android.graphics.Color.green(initialColorInt).toFloat()) }
    var blue by remember { mutableFloatStateOf(android.graphics.Color.blue(initialColorInt).toFloat()) }
    var hexInput by remember { mutableStateOf(initialHex.uppercase()) }

    fun syncHexFromRgb() {
        val r = red.roundToInt().coerceIn(0, 255)
        val g = green.roundToInt().coerceIn(0, 255)
        val b = blue.roundToInt().coerceIn(0, 255)
        hexInput = String.format("#%02X%02X%02X", r, g, b)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)

                // Color Preview Bar
                val previewColor = Color(red.roundToInt(), green.roundToInt(), blue.roundToInt())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(previewColor)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hexInput,
                        color = if ((red * 0.299 + green * 0.587 + blue * 0.114) > 160) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Preset Swatches Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetColors.chunked(8).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowColors.forEach { hex ->
                                val cInt = CardPdfGenerator.parseColorSafe(hex)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(cInt))
                                        .border(
                                            width = if (hex.equals(hexInput, true)) 2.5.dp else 1.dp,
                                            color = if (hex.equals(hexInput, true)) PrimaryBlue else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            red = android.graphics.Color.red(cInt).toFloat()
                                            green = android.graphics.Color.green(cInt).toFloat()
                                            blue = android.graphics.Color.blue(cInt).toFloat()
                                            hexInput = hex
                                        }
                                )
                            }
                        }
                    }
                }

                // RGB Sliders
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("أحمر (R): ${red.toInt()}", fontSize = 11.sp, color = Color(0xFFDC2626))
                    Slider(value = red, onValueChange = { red = it; syncHexFromRgb() }, valueRange = 0f..255f)
                    Text("أخضر (G): ${green.toInt()}", fontSize = 11.sp, color = Color(0xFF16A34A))
                    Slider(value = green, onValueChange = { green = it; syncHexFromRgb() }, valueRange = 0f..255f)
                    Text("أزرق (B): ${blue.toInt()}", fontSize = 11.sp, color = Color(0xFF2563EB))
                    Slider(value = blue, onValueChange = { blue = it; syncHexFromRgb() }, valueRange = 0f..255f)
                }

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { v ->
                        hexInput = v
                        if (v.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                            val c = CardPdfGenerator.parseColorSafe(v)
                            red = android.graphics.Color.red(c).toFloat()
                            green = android.graphics.Color.green(c).toFloat()
                            blue = android.graphics.Color.blue(c).toFloat()
                        }
                    },
                    label = { Text("كود اللون (Hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = { onConfirm(hexInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اختيار اللون")
                    }
                }
            }
        }
    }
}

@Composable
fun PositionedElementSectionCard(
    sectionTitle: String,
    toggleLabel: String,
    textFieldLabel: String,
    sizeStepperLabel: String,
    icon: ImageVector,
    iconTint: Color,
    isEnabled: Boolean,
    onToggleChange: (Boolean) -> Unit,
    textValue: String,
    onTextChange: (String) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    xPercent: Float,
    yPercent: Float,
    onMove: (Float, Float) -> Unit
) {
    DesignerSectionCard(
        title = sectionTitle,
        icon = icon,
        iconTint = iconTint,
        trailingSwitchChecked = isEnabled,
        onTrailingSwitchChange = onToggleChange
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(toggleLabel, fontSize = 12.sp, color = TextSecondaryLight)
            Text(
                text = if (isEnabled) "مفعل ✓" else "معطل",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color(0xFF16A34A) else Color(0xFF94A3B8)
            )
        }

        AnimatedVisibility(visible = isEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = onTextChange,
                    label = { Text(textFieldLabel, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                FloatStepperRow(
                    label = sizeStepperLabel,
                    value = fontSize,
                    minValue = 5.0f,
                    maxValue = 28.0f,
                    step = 0.5f,
                    onValueChange = onFontSizeChange
                )

                DPadController(
                    title = "تحريك ($sectionTitle) داخل الكرت",
                    xPercent = xPercent,
                    yPercent = yPercent,
                    onMove = onMove
                )
            }
        }
    }
}

@Composable
fun LiveCardPreviewBox(
    template: CardTemplateEntity,
    bgBitmap: Bitmap?,
    logoBitmap: Bitmap?,
    sampleCard: GeneratedCardPrintItem,
    boxHeight: Dp = 112.dp,
    modifier: Modifier = Modifier
) {
    val borderColor = remember(template.borderColorHex) {
        Color(CardPdfGenerator.parseColorSafe(template.borderColorHex, android.graphics.Color.BLACK))
    }
    val fontColor = remember(template.fontColorHex) {
        Color(CardPdfGenerator.parseColorSafe(template.fontColorHex, android.graphics.Color.rgb(17, 24, 39)))
    }
    val borderStrokeDp = if (template.enableBorder) (template.borderSizeMm * 2.5f).coerceIn(0.6f, 6f).dp else 0.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .then(
                if (template.enableBorder) {
                    Modifier.border(borderStrokeDp, borderColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier.border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                }
            )
    ) {
        val boxW = maxWidth
        val boxH = maxHeight

        // 1. Background image filling card without margins
        if (template.enableBackground && bgBitmap != null) {
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                contentDescription = "خلفية الكرت",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Logo
        if (template.enableLogo && logoBitmap != null) {
            val lw = (template.logoWidthMm * 2.4f).coerceIn(12f, 80f).dp
            val lh = (template.logoHeightMm * 2.4f).coerceIn(12f, 70f).dp
            val cx = boxW * (template.logoXPercent / 100f) - (lw / 2f)
            val cy = boxH * (template.logoYPercent / 100f) - (lh / 2f)
            Image(
                bitmap = logoBitmap.asImageBitmap(),
                contentDescription = "شعار الكرت",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(x = cx, y = cy)
                    .size(width = lw, height = lh)
            )
        }

        // 3. Custom Title
        if (template.enableCustomTitle && template.customTitleText.isNotBlank()) {
            Text(
                text = template.customTitleText,
                color = fontColor,
                fontSize = (template.fontSize * 0.9f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp, start = 8.dp, end = 8.dp)
            )
        }

        // 4. Username / Card Code (with exact X/Y positioning)
        if (template.showUsername) {
            PositionedPreviewText(
                text = sampleCard.username,
                xPercent = template.userXPercent,
                yPercent = template.userYPercent,
                boxW = boxW,
                boxH = boxH,
                fontSizeSp = template.fontSize,
                color = fontColor,
                isBold = template.isFontBold,
                monospace = true
            )
        }

        // 5. Password
        if (template.showPassword) {
            val txt = CardPdfGenerator.resolveFieldText(template.passwordText, sampleCard, template)
            PositionedPreviewText(txt, template.passwordXPercent, template.passwordYPercent, boxW, boxH, template.passwordFontSize, fontColor, false)
        }

        // 6. Payment Number
        if (template.showPayment) {
            val txt = CardPdfGenerator.resolveFieldText(template.paymentText, sampleCard, template)
            PositionedPreviewText(txt, template.paymentXPercent, template.paymentYPercent, boxW, boxH, template.paymentFontSize, fontColor, false)
        }

        // 7. Serial Number
        if (template.showSerial) {
            val txt = CardPdfGenerator.resolveFieldText(template.serialText, sampleCard, template)
            PositionedPreviewText(txt, template.serialXPercent, template.serialYPercent, boxW, boxH, template.serialFontSize, fontColor, false)
        }

        // 8. Package Price
        if (template.showPrice) {
            val txt = CardPdfGenerator.resolveFieldText(template.priceTagText, sampleCard, template)
            PositionedPreviewText(txt, template.priceXPercent, template.priceYPercent, boxW, boxH, template.priceFontSize, fontColor, true)
        }

        // 9. Point of Sale (POS)
        if (template.showPos) {
            val txt = CardPdfGenerator.resolveFieldText(template.posText, sampleCard, template)
            PositionedPreviewText(txt, template.posXPercent, template.posYPercent, boxW, boxH, template.posFontSize, fontColor, false)
        }

        // 10. Barcode
        if (template.enableBarcode) {
            val bars = remember(sampleCard.username) { CardPdfGenerator.encodeCode128Pattern(sampleCard.username) }
            val barW = boxW * 0.56f
            val barH = 20.dp
            val bx = boxW * (template.barcodeXPercent / 100f) - (barW / 2f)
            val by = boxH * (template.barcodeYPercent / 100f) - (barH / 2f)
            Canvas(
                modifier = Modifier
                    .offset(x = bx, y = by)
                    .size(width = barW, height = barH)
                    .background(Color.White.copy(alpha = 0.85f))
            ) {
                val totalUnits = bars.sum().coerceAtLeast(1)
                val unitW = size.width / totalUnits
                var curX = 0f
                bars.forEachIndexed { idx, u ->
                    val w = u * unitW
                    if (idx % 2 == 0) {
                        drawRect(Color.Black, topLeft = Offset(curX, 0f), size = Size(w, size.height))
                    }
                    curX += w
                }
            }
        }
    }
}

@Composable
private fun PositionedPreviewText(
    text: String,
    xPercent: Float,
    yPercent: Float,
    boxW: Dp,
    boxH: Dp,
    fontSizeSp: Float,
    color: Color,
    isBold: Boolean,
    monospace: Boolean = false
) {
    // Center anchor around (xPercent%, yPercent%) using Box wrapper
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        val estHalfW = (boxW * 0.45f)
        val cx = (boxW * (xPercent / 100f)) - estHalfW
        val cy = (boxH * (yPercent / 100f)) - (fontSizeSp * 0.7f).dp
        Box(
            modifier = Modifier
                .offset(x = cx, y = cy)
                .width(boxW * 0.9f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = fontSizeSp.coerceIn(6f, 26f).sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun A4SheetPreviewDialog(
    template: CardTemplateEntity,
    bgBitmap: Bitmap?,
    logoBitmap: Bitmap?,
    cards: List<GeneratedCardPrintItem>,
    onDismiss: () -> Unit,
    onExportAndOpenPdf: () -> Unit
) {
    val cols = template.columnsPerPage.coerceIn(1, 12)
    val rows = template.rowsPerPage.coerceIn(1, 35)
    val perPage = cols * rows
    val borderColor = remember(template.borderColorHex) {
        Color(CardPdfGenerator.parseColorSafe(template.borderColorHex, android.graphics.Color.BLACK))
    }
    val fontColor = remember(template.fontColorHex) {
        Color(CardPdfGenerator.parseColorSafe(template.fontColorHex, android.graphics.Color.rgb(17, 24, 39)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("معاينة صفحة الطباعة A4", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                        Text("${cols} أعمدة × ${rows} صفوف (${perPage} كرت بالورقة) • إجمالي ${cards.size} كرت", fontSize = 11.sp, color = TextSecondaryLight)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Exact A4 Aspect Ratio Sheet Simulation
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f / 1.414f)
                            .shadow(6.dp, RoundedCornerShape(3.dp))
                            .background(Color.White, RoundedCornerShape(3.dp))
                            .padding(6.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (template.showPageNote && template.pageNote.isNotBlank()) {
                                Text(
                                    text = template.pageNote,
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val gapH = (template.marginHorizontalMm * 0.8f).coerceAtLeast(0.5f).dp
                            val gapV = (template.marginVerticalMm * 0.8f).coerceAtLeast(0.5f).dp

                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(gapV)
                            ) {
                                val previewRows = rows.coerceAtMost(20)
                                val previewCols = cols.coerceAtMost(8)
                                for (r in 0 until previewRows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(gapH)
                                    ) {
                                        for (c in 0 until previewCols) {
                                            val cardIdx = r * cols + c
                                            val cardItem = cards.getOrNull(cardIdx)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(Color.White)
                                                    .then(
                                                        if (template.enableBorder) {
                                                            Modifier.border(
                                                                width = (template.borderSizeMm * 0.8f).coerceIn(0.3f, 2f).dp,
                                                                color = borderColor
                                                            )
                                                        } else {
                                                            Modifier.border(0.2.dp, Color(0xFFE2E8F0))
                                                        }
                                                    )
                                            ) {
                                                if (cardItem != null) {
                                                    if (template.enableBackground && bgBitmap != null) {
                                                        Image(
                                                            bitmap = bgBitmap.asImageBitmap(),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    if (template.showUsername) {
                                                        val xFrac = (template.userXPercent / 100f).coerceIn(0.1f, 0.9f)
                                                        val yFrac = (template.userYPercent / 100f).coerceIn(0.1f, 0.9f)
                                                        Box(modifier = Modifier.fillMaxSize()) {
                                                            Text(
                                                                text = cardItem.username,
                                                                fontSize = (if (rows > 12) 4.sp else 5.5.sp),
                                                                fontWeight = if (template.isFontBold) FontWeight.Bold else FontWeight.Normal,
                                                                color = fontColor,
                                                                maxLines = 1,
                                                                modifier = Modifier.align(
                                                                    androidx.compose.ui.BiasAlignment(
                                                                        horizontalBias = (xFrac - 0.5f) * 2f,
                                                                        verticalBias = (yFrac - 0.5f) * 2f
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (template.showPageNumber) {
                                val totalPages = ((cards.size + perPage - 1) / perPage).coerceAtLeast(1)
                                Text(
                                    text = "Page 1 / $totalPages",
                                    fontSize = 5.5.sp,
                                    color = TextSecondaryLight,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onExportAndOpenPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("توليد وفتح ملف PDF على الهاتف 🖨️", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
