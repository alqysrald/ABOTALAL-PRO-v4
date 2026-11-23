package com.example.ui.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CardTemplateEntity
import com.example.data.model.GeneratedCardPrintItem
import java.io.File
import java.io.FileOutputStream

object CardPdfGenerator {

    const val MM_TO_PT = 72f / 25.4f // ~2.83465f points per mm
    const val A4_WIDTH_PT = 595f
    const val A4_HEIGHT_PT = 842f

    fun parseColorSafe(hex: String, fallback: Int = android.graphics.Color.BLACK): Int {
        return try {
            val clean = if (hex.startsWith("#")) hex else "#$hex"
            android.graphics.Color.parseColor(clean)
        } catch (_: Exception) {
            fallback
        }
    }

    data class CardSheetMetrics(
        val cols: Int,
        val rows: Int,
        val pageOuterMarginPt: Float,
        val headerSpacePt: Float,
        val footerSpacePt: Float,
        val gapHPt: Float,
        val gapVPt: Float,
        val cardWidthPt: Float,
        val cardHeightPt: Float,
        val aspectRatio: Float,
        val scaleFactor: Float
    )

    fun computeSheetMetrics(template: CardTemplateEntity): CardSheetMetrics {
        val cols = template.columnsPerPage.coerceIn(1, 12)
        val rows = template.rowsPerPage.coerceIn(1, 35)
        val pageOuterMargin = 14f
        val headerSpace = if (template.showPageNote || template.showPageNumber) 24f else 6f
        val footerSpace = if (template.showPageNumber) 18f else 6f
        val gapH = (template.marginHorizontalMm * MM_TO_PT).coerceAtLeast(0f)
        val gapV = (template.marginVerticalMm * MM_TO_PT).coerceAtLeast(0f)
        val availW = A4_WIDTH_PT - (2f * pageOuterMargin)
        val availH = A4_HEIGHT_PT - (2f * pageOuterMargin) - headerSpace - footerSpace
        val cardW = ((availW - (cols - 1) * gapH) / cols).coerceAtLeast(18f)
        val cardH = ((availH - (rows - 1) * gapV) / rows).coerceAtLeast(14f)
        val scaleFactor = if (template.scaleToA4) (cardH / 120f).coerceIn(0.32f, 1.35f) else 1.0f
        return CardSheetMetrics(
            cols = cols,
            rows = rows,
            pageOuterMarginPt = pageOuterMargin,
            headerSpacePt = headerSpace,
            footerSpacePt = footerSpace,
            gapHPt = gapH,
            gapVPt = gapV,
            cardWidthPt = cardW,
            cardHeightPt = cardH,
            aspectRatio = (cardW / cardH).coerceIn(0.6f, 6.0f),
            scaleFactor = scaleFactor
        )
    }

    fun persistPickedImage(context: Context, uri: Uri, prefix: String): String? {
        return try {
            val isLogo = prefix.contains("logo", ignoreCase = true)
            val subFolder = if (isLogo) "card_assets/logos" else "card_assets/backgrounds"
            val dir = File(context.filesDir, subFolder)
            if (!dir.exists()) dir.mkdirs()
            val ext = if (isLogo) "png" else "jpg"
            val outFile = File(dir, "${prefix}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) {
                    FileOutputStream(outFile).use { out ->
                        if (isLogo) {
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        } else {
                            bmp.compress(Bitmap.CompressFormat.JPEG, 94, out)
                        }
                    }
                    return outFile.absolutePath
                }
            }
            // Fallback raw copy if decodeStream failed
            context.contentResolver.openInputStream(uri)?.use { rawIn ->
                FileOutputStream(outFile).use { out ->
                    rawIn.copyTo(out)
                }
                if (outFile.length() > 0L) return outFile.absolutePath
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listSavedBackgrounds(context: Context): List<File> {
        val bgDir = File(context.filesDir, "card_assets/backgrounds")
        val legacyDir = File(context.filesDir, "card_assets")
        val fromNew = if (bgDir.exists()) {
            bgDir.listFiles()?.filter { it.isFile && it.length() > 0L }.orEmpty()
        } else emptyList()
        val fromLegacy = if (legacyDir.exists()) {
            legacyDir.listFiles()?.filter { it.isFile && it.name.startsWith("card_bg") && it.length() > 0L }.orEmpty()
        } else emptyList()
        return (fromNew + fromLegacy).distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
    }

    fun listSavedLogos(context: Context): List<File> {
        val logoDir = File(context.filesDir, "card_assets/logos")
        val legacyDir = File(context.filesDir, "card_assets")
        val fromNew = if (logoDir.exists()) {
            logoDir.listFiles()?.filter { it.isFile && it.length() > 0L }.orEmpty()
        } else emptyList()
        val fromLegacy = if (legacyDir.exists()) {
            legacyDir.listFiles()?.filter { it.isFile && it.name.startsWith("card_logo") && it.length() > 0L }.orEmpty()
        } else emptyList()
        return (fromNew + fromLegacy).distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
    }

    fun deleteSavedAsset(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }

    fun loadBitmapFromPathOrUri(context: Context, pathOrUri: String): Bitmap? {
        if (pathOrUri.isBlank()) return null
        return try {
            val file = File(pathOrUri)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun sampleUsername(digits: Int): String {
        val safeDigits = digits.coerceIn(1, 20)
        val base = "12345678901234567890"
        return base.take(safeDigits)
    }

    fun resolveFieldText(
        rawTemplate: String,
        card: GeneratedCardPrintItem,
        template: CardTemplateEntity
    ): String {
        val priceFallback = template.categoryChip.ifBlank { card.price }
        return rawTemplate
            .replace("{username}", card.username)
            .replace("{password}", card.password)
            .replace("{payment}", card.paymentNumber.ifBlank { "PAY-1042" })
            .replace("{serial}", card.serial.ifBlank { "SN-0001" })
            .replace("{price}", card.price.ifBlank { priceFallback })
            .replace("{pos}", card.posName.ifBlank { "نقطة البيع الرئيسية" })
            .replace("{profile}", card.profileName)
    }

    fun generateSampleCards(template: CardTemplateEntity): List<GeneratedCardPrintItem> {
        val total = template.cardsCount.coerceIn(1, 500)
        val digits = template.usernameDigits.coerceIn(4, 18)
        val linkedProfile = template.linkedUmProfile.ifBlank {
            template.linkedHotspotProfile.ifBlank { template.profileName }
        }
        val priceText = template.categoryChip.replace("فئة ", "")

        return (1..total).map { idx ->
            val code = if (idx == 1) {
                sampleUsername(digits)
            } else {
                val num = (1000000000L + idx * 7919L).toString()
                num.padStart(digits, '8').takeLast(digits)
            }
            GeneratedCardPrintItem(
                username = code,
                password = code.takeLast(6),
                profileName = linkedProfile,
                price = priceText,
                serial = String.format("SN-%04d", idx),
                paymentNumber = "B-2026",
                posName = "نقطة البيع #1"
            )
        }
    }

    /**
     * Generates a genuine ISO-15417 Code 128B module bar pattern (alternating bar/space widths).
     */
    fun encodeCode128Pattern(text: String): List<Int> {
        val patterns = arrayOf(
            "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
            "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
            "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
            "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
            "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
            "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
            "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
            "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
            "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
            "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
            "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
        )
        val safeText = text.ifBlank { "1234567890" }.filter { it.code in 32..126 }.ifEmpty { "1234567890" }
        val result = mutableListOf<Int>()
        // Start B = 104
        var checksum = 104
        patterns[104].forEach { result.add(it.digitToInt()) }
        safeText.forEachIndexed { idx, ch ->
            val codeVal = (ch.code - 32).coerceIn(0, 95)
            checksum += (idx + 1) * codeVal
            patterns[codeVal].forEach { result.add(it.digitToInt()) }
        }
        val checkDigit = checksum % 103
        patterns[checkDigit].forEach { result.add(it.digitToInt()) }
        // Stop = 106
        patterns[106].forEach { result.add(it.digitToInt()) }
        return result
    }

    fun generateA4CardsPdf(
        context: Context,
        template: CardTemplateEntity,
        cardsInput: List<GeneratedCardPrintItem>? = null
    ): File? {
        return try {
            val cards = if (cardsInput.isNullOrEmpty()) {
                generateSampleCards(template)
            } else {
                cardsInput
            }

            val metrics = computeSheetMetrics(template)
            val cols = metrics.cols
            val rows = metrics.rows
            val perPage = (cols * rows).coerceAtLeast(1)

            val bgBitmap = if (template.enableBackground && template.bgImageUri.isNotBlank()) {
                loadBitmapFromPathOrUri(context, template.bgImageUri)
            } else null

            val logoBitmap = if (template.enableLogo && template.logoImageUri.isNotBlank()) {
                loadBitmapFromPathOrUri(context, template.logoImageUri)
            } else null

            val doc = PdfDocument()
            val pages = cards.chunked(perPage)
            val totalPages = pages.size.coerceAtLeast(1)

            val pageOuterMargin = metrics.pageOuterMarginPt
            val headerSpace = metrics.headerSpacePt
            val gapH = metrics.gapHPt
            val gapV = metrics.gapVPt
            val cardW = metrics.cardWidthPt
            val cardH = metrics.cardHeightPt
            val scaleFactor = metrics.scaleFactor

            val paint = Paint().apply { isAntiAlias = true }
            val borderColor = parseColorSafe(template.borderColorHex, android.graphics.Color.BLACK)
            val fontColor = parseColorSafe(template.fontColorHex, android.graphics.Color.rgb(17, 24, 39))

            pages.forEachIndexed { pageIdx, pageCards ->
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT.toInt(), A4_HEIGHT_PT.toInt(), pageIdx + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                // Top Page Note & Info
                if (template.showPageNote && template.pageNote.isNotBlank()) {
                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.rgb(30, 41, 59)
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(template.pageNote, A4_WIDTH_PT - pageOuterMargin, pageOuterMargin + 10f, paint)
                }

                if (template.showPageNumber) {
                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.DKGRAY
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText("Page ${pageIdx + 1} / $totalPages", pageOuterMargin, A4_HEIGHT_PT - 10f, paint)
                }

                pageCards.forEachIndexed { idx, card ->
                    val r = idx / cols
                    val c = idx % cols

                    val left = pageOuterMargin + c * (cardW + gapH)
                    val top = pageOuterMargin + headerSpace + r * (cardH + gapV)
                    val right = left + cardW
                    val bottom = top + cardH
                    val cardRect = RectF(left, top, right, bottom)

                    canvas.save()
                    canvas.clipRect(cardRect)

                    // 1. Card Background (White base or Full-bleed Image)
                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawRect(cardRect, paint)

                    if (bgBitmap != null) {
                        val srcRect = computeCenterCropSrcRect(bgBitmap.width, bgBitmap.height, cardW, cardH)
                        canvas.drawBitmap(bgBitmap, srcRect, cardRect, null)
                    }

                    // 2. Logo
                    if (template.enableLogo && logoBitmap != null) {
                        val logoW = (template.logoWidthMm * MM_TO_PT * scaleFactor).coerceIn(6f, cardW * 0.7f)
                        val logoH = (template.logoHeightMm * MM_TO_PT * scaleFactor).coerceIn(6f, cardH * 0.7f)
                        val cx = left + (template.logoXPercent / 100f) * cardW
                        val cy = top + (template.logoYPercent / 100f) * cardH
                        val logoDest = RectF(cx - logoW / 2f, cy - logoH / 2f, cx + logoW / 2f, cy + logoH / 2f)
                        canvas.drawBitmap(logoBitmap, null, logoDest, null)
                    }

                    // 3. Custom Title
                    if (template.enableCustomTitle && template.customTitleText.isNotBlank()) {
                        paint.style = Paint.Style.FILL
                        paint.color = fontColor
                        paint.textSize = (template.fontSize * 0.85f * scaleFactor).coerceAtLeast(4f)
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        paint.textAlign = Paint.Align.CENTER
                        val fm = paint.fontMetrics
                        val centerOffset = -(fm.ascent + fm.descent) / 2f
                        canvas.drawText(
                            template.customTitleText,
                            left + cardW / 2f,
                            top + (cardH * 0.14f) + centerOffset,
                            paint
                        )
                    }

                    // 4. Username / Card Code
                    if (template.showUsername) {
                        paint.style = Paint.Style.FILL
                        paint.color = fontColor
                        paint.textSize = (template.fontSize * scaleFactor).coerceAtLeast(4.5f)
                        paint.typeface = Typeface.create(
                            Typeface.DEFAULT,
                            if (template.isFontBold) Typeface.BOLD else Typeface.NORMAL
                        )
                        paint.textAlign = Paint.Align.CENTER
                        val fm = paint.fontMetrics
                        val centerOffset = -(fm.ascent + fm.descent) / 2f
                        val ux = left + (template.userXPercent / 100f) * cardW
                        val uy = top + (template.userYPercent / 100f) * cardH + centerOffset
                        canvas.drawText(card.username, ux, uy, paint)
                    }

                    // 5. Password
                    if (template.showPassword) {
                        val text = resolveFieldText(template.passwordText, card, template)
                        drawPositionedText(canvas, paint, text, left, top, cardW, cardH, template.passwordXPercent, template.passwordYPercent, template.passwordFontSize * scaleFactor, fontColor, false)
                    }

                    // 6. Payment Number
                    if (template.showPayment) {
                        val text = resolveFieldText(template.paymentText, card, template)
                        drawPositionedText(canvas, paint, text, left, top, cardW, cardH, template.paymentXPercent, template.paymentYPercent, template.paymentFontSize * scaleFactor, fontColor, false)
                    }

                    // 7. Serial Number
                    if (template.showSerial) {
                        val text = resolveFieldText(template.serialText, card, template)
                        drawPositionedText(canvas, paint, text, left, top, cardW, cardH, template.serialXPercent, template.serialYPercent, template.serialFontSize * scaleFactor, fontColor, false)
                    }

                    // 8. Package Price
                    if (template.showPrice) {
                        val text = resolveFieldText(template.priceTagText, card, template)
                        drawPositionedText(canvas, paint, text, left, top, cardW, cardH, template.priceXPercent, template.priceYPercent, template.priceFontSize * scaleFactor, fontColor, true)
                    }

                    // 9. Point of Sale (POS)
                    if (template.showPos) {
                        val text = resolveFieldText(template.posText, card, template)
                        drawPositionedText(canvas, paint, text, left, top, cardW, cardH, template.posXPercent, template.posYPercent, template.posFontSize * scaleFactor, fontColor, false)
                    }

                    // 10. Barcode (Code 128)
                    if (template.enableBarcode) {
                        val bars = encodeCode128Pattern(card.username)
                        val totalUnits = bars.sum().coerceAtLeast(1)
                        val targetBarW = (cardW * 0.65f).coerceAtLeast(24f)
                        val targetBarH = (cardH * 0.22f).coerceIn(6f, 24f)
                        val unitW = targetBarW / totalUnits
                        val startX = left + (template.barcodeXPercent / 100f) * cardW - targetBarW / 2f
                        val startY = top + (template.barcodeYPercent / 100f) * cardH - targetBarH / 2f

                        paint.style = Paint.Style.FILL
                        var curX = startX
                        bars.forEachIndexed { bIdx, units ->
                            val w = units * unitW
                            if (bIdx % 2 == 0) {
                                paint.color = android.graphics.Color.BLACK
                                canvas.drawRect(curX, startY, curX + w, startY + targetBarH, paint)
                            }
                            curX += w
                        }
                    }

                    canvas.restore()

                    // 11. Card Border
                    if (template.enableBorder) {
                        paint.style = Paint.Style.STROKE
                        paint.color = borderColor
                        paint.strokeWidth = (template.borderSizeMm * MM_TO_PT).coerceAtLeast(0.3f)
                        canvas.drawRect(cardRect, paint)
                    }
                }

                doc.finishPage(page)
            }

            val appFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TalalPro/Cards")
            if (!appFolder.exists()) appFolder.mkdirs()
            val safeName = template.templateName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "Design" }
            val file = File(appFolder, "TalalPro_${safeName}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out -> doc.writeTo(out) }
            doc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawPositionedText(
        canvas: android.graphics.Canvas,
        paint: Paint,
        text: String,
        cardLeft: Float,
        cardTop: Float,
        cardW: Float,
        cardH: Float,
        xPercent: Float,
        yPercent: Float,
        textSizePt: Float,
        color: Int,
        bold: Boolean
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = textSizePt.coerceAtLeast(3.8f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        val fm = paint.fontMetrics
        val centerOffset = -(fm.ascent + fm.descent) / 2f
        val x = cardLeft + (xPercent / 100f) * cardW
        val y = cardTop + (yPercent / 100f) * cardH + centerOffset
        canvas.drawText(text, x, y, paint)
    }

    private fun computeCenterCropSrcRect(
        bmpW: Int,
        bmpH: Int,
        destW: Float,
        destH: Float
    ): Rect {
        val bmpRatio = bmpW.toFloat() / bmpH.toFloat()
        val destRatio = destW / destH
        return if (bmpRatio > destRatio) {
            val newW = (bmpH * destRatio).toInt()
            val xOff = (bmpW - newW) / 2
            Rect(xOff, 0, xOff + newW, bmpH)
        } else {
            val newH = (bmpW / destRatio).toInt()
            val yOff = (bmpH - newH) / 2
            Rect(0, yOff, bmpW, yOff + newH)
        }
    }

    fun openPdfOnDevice(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "معاينة وطباعة ملف الكروت PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "تم حفظ الملف في: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}
