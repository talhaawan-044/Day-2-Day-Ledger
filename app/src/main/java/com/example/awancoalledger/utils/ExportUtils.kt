package com.example.awancoalledger.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.awancoalledger.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    fun generateAndSharePdf(
            context: Context,
            details: PartyWithDetails,
            balance: Double, // This is expected to be the final balance
            startDate: Long? = null,
            endDate: Long? = null,
            businessName: String,
            ownerName: String,
            logoUri: String? = null,
            signatureUri: String? = null
    ) {
        try {
            val pdfDocument = PdfDocument()
            val df = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val nf = DecimalFormat("#,###")

            // Enhanced Color Palette - Eye-pleasing and professional
            val colorSlate900 = 0xFF0F172A.toInt() // Deep dark for headers
            val colorSlate700 = 0xFF334155.toInt() // Dark gray for text
            val colorSlate600 = 0xFF475569.toInt() // Gray for secondary text
            val colorSlate500 = 0xFF64748B.toInt() // Medium gray for labels
            val colorSlate300 = 0xFFCBD5E1.toInt() // Light gray for borders
            val colorSlate100 = 0xFFF1F5F9.toInt() // Very light gray for alternating rows
            val colorHeaderBg = 0xFFF8FAFC.toInt() // Softest background
            val colorSuccess = 0xFF059669.toInt() // Emerald green for Receivable
            val colorError = 0xFFDC2626.toInt() // Red for Payable
            val colorWarning = 0xFFF59E0B.toInt() // Amber for warnings
            val colorBlue = 0xFF3B82F6.toInt() // Blue for info
            val colorPurple = 0xFF9333EA.toInt() // Purple for highlights

            // Helper to load Bitmaps
            fun loadBitmap(uriString: String?): Bitmap? {
                if (uriString == null) return null
                return try {
                    val uri = Uri.parse(uriString)
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val logoBitmap = loadBitmap(logoUri)
            val signatureBitmap = loadBitmap(signatureUri)

            // --- DATA PREPARATION ---
            // Sort chronologically for balance calculation; display order is reversed later
            val chronologicalTransactions =
                    (details.entries.map { it to "entry" } +
                                    details.payments.map { it to "payment" })
                            .sortedBy {
                                if (it.first is LedgerEntry) (it.first as LedgerEntry).date
                                else (it.first as Payment).date
                            }

            var openingBalance = 0.0
            val rangeItems = mutableListOf<Pair<Any, String>>()

            chronologicalTransactions.forEach { (item, type) ->
                val date = if (item is LedgerEntry) item.date else (item as Payment).date
                val amount =
                        if (type == "entry") {
                            val entry = item as LedgerEntry
                            ((entry.weight ?: 0.0) * (entry.rate ?: 0.0)) + (entry.fare ?: 0.0)
                        } else {
                            (item as Payment).amount
                        }

                val contribution =
                        if (details.party.type == PartyType.BUYER) {
                            if (type == "entry") amount else -amount
                        } else {
                            if (type == "entry") -amount else amount
                        }

                if (startDate != null && date < startDate) {
                    openingBalance += contribution
                } else if (endDate == null || date <= endDate) {
                    rangeItems.add(item to type)
                }
            }

            // Reverse rangeItems so latest entries are at the top of the PDF
            rangeItems.reverse()

            // --- PRE-CALCULATE TOTAL PAGES ---
            // Count how many rows fit per page to determine total page count
            val firstPageDataStart = 530f // y position after header+party+summary+table header
            val subsequentPageDataStart = 190f // y position after header+table header on subsequent pages
            val maxY = 750f
            val rowH = 30f
            var tempY = firstPageDataStart
            var calculatedPages = 1
            for (i in rangeItems.indices) {
                if (tempY > maxY) {
                    calculatedPages++
                    tempY = subsequentPageDataStart
                }
                tempY += rowH
            }
            // Account for footer needing space
            if (tempY > 680f) calculatedPages++
            val totalPages = calculatedPages

            // --- START PDF PAGE ---
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas
            val paint =
                    Paint().apply {
                        isAntiAlias = true
                        isSubpixelText = true
                        hinting = Paint.HINTING_ON
                    }

            // Use DEFAULT-based typefaces for consistent glyph metrics & kerning
            val tfRegular = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val tfBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val tfItalic = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

            // Helper to reset paint text state before every drawText call
            // Prevents letterSpacing / textScaleX drift between draws
            fun Paint.resetText() {
                letterSpacing = 0f
                textScaleX = 1f
            }

            // Helper function to draw rounded rectangles with shadows
            fun drawBoxWithShadow(
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float,
                    radius: Float,
                    fillColor: Int,
                    borderColor: Int
            ) {
                // Shadow effect
                paint.color = 0x30000000.toInt()
                canvas.drawRoundRect(
                        left + 2f,
                        top + 2f,
                        right + 2f,
                        bottom + 2f,
                        radius,
                        radius,
                        paint
                )

                // Fill
                paint.style = Paint.Style.FILL
                paint.color = fillColor
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)

                // Border
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                paint.color = borderColor
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
                paint.style = Paint.Style.FILL
            }

            fun drawHeader(pageNum: Int) {
                paint.resetText()
                // Background
                paint.color = colorHeaderBg
                canvas.drawRect(0f, 0f, 595f, 140f, paint)

                // Decorative top bar
                paint.color = colorBlue
                canvas.drawRect(0f, 0f, 595f, 8f, paint)

                // Logo or placeholder
                if (logoBitmap != null) {
                    val dst = RectF(25f, 30f, 95f, 100f)
                    paint.color = Color.WHITE
                    canvas.drawRoundRect(23f, 28f, 97f, 102f, 12f, 12f, paint)
                    canvas.drawBitmap(logoBitmap, null, dst, paint)
                } else {
                    paint.color = 0xFFE0E7FF.toInt()
                    canvas.drawRoundRect(25f, 30f, 95f, 100f, 12f, 12f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = colorBlue
                    canvas.drawRoundRect(25f, 30f, 95f, 100f, 12f, 12f, paint)
                    paint.style = Paint.Style.FILL
                }

                // Business Name
                paint.resetText()
                paint.color = colorSlate900
                paint.textSize = 24f
                paint.typeface = tfBold
                canvas.drawText(businessName, if (logoBitmap != null) 110f else 25f, 55f, paint)

                // Owner info
                paint.resetText()
                paint.color = colorSlate500
                paint.textSize = 11f
                paint.typeface = tfRegular
                canvas.drawText(
                        "$ownerName | ${details.owner.phone ?: "N/A"}",
                        if (logoBitmap != null) 110f else 25f,
                        75f,
                        paint
                )

                // Document Title - Right aligned
                paint.resetText()
                paint.color = colorSlate900
                paint.textSize = 15f
                paint.typeface = tfRegular
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("STATEMENT", 570f, 60f, paint)

                paint.resetText()
                paint.color = colorSlate500
                paint.textSize = 10f
                paint.typeface = tfRegular
                canvas.drawText("Printed: ${df.format(Date())}", 570f, 80f, paint)

                // Page number with correct total
                paint.resetText()
                paint.textSize = 9f
                paint.color = colorSlate500
                canvas.drawText("Page $pageNum of $totalPages", 570f, 100f, paint)
                paint.textAlign = Paint.Align.LEFT

                // Bottom separator line
                paint.color = colorSlate300
                paint.strokeWidth = 1f
                canvas.drawLine(0f, 140f, 595f, 140f, paint)
            }

            drawHeader(pageNumber)

            var y = 165f

            // --- PARTY INFO SECTION with better visual design ---
            drawBoxWithShadow(20f, y, 575f, y + 70f, 10f, Color.WHITE, colorSlate300)

            // Party name - larger and bolder
            paint.resetText()
            paint.color = colorSlate900
            paint.textSize = 18f
            paint.typeface = tfBold
            canvas.drawText(details.party.name, 35f, y + 25f, paint)

            // Party details in a more organized way
            paint.resetText()
            paint.color = colorSlate700
            paint.textSize = 11f
            paint.typeface = tfRegular
            val partyTypeLabel =
                    //if (details.party.type == PartyType.BUYER) "Customer (Buyer)" else "Supplier"
            //canvas.drawText("Type: $partyTypeLabel", 35f, y + 45f, paint)

            if (details.party.phone != null) {
                canvas.drawText("Phone:  ${details.party.phone}", 220f, y + 45f, paint)
            } else {
                canvas.drawText("Phone:  xxxxxxxxxxx", 220f, y + 45f, paint)
            }

            if (details.party.address != null) {
                paint.textSize = 10f
                canvas.drawText("Location:  ${details.party.address}", 35f, y + 45f, paint)
            }

            // Date Range - right side with better styling
            paint.resetText()
            paint.textSize = 11f
            paint.typeface = tfBold
            paint.textAlign = Paint.Align.RIGHT
            paint.color = colorBlue
            val startStr = if (startDate != null) df.format(Date(startDate)) else "Beginning"
            val endStr = if (endDate != null) df.format(Date(endDate)) else df.format(Date())
            canvas.drawText("Statement Range:", 570f, y + 25f, paint)
            paint.resetText()
            paint.textSize = 10f
            paint.typeface = tfRegular
            paint.color = colorSlate700
            canvas.drawText("$startStr", 570f, y + 42f, paint)
            canvas.drawText("to $endStr", 570f, y + 57f, paint)
            paint.textAlign = Paint.Align.LEFT

            y += 90f

            // --- ENHANCED SUMMARY BOXES - Much more prominent and clear ---

            // Helper to format balance with clear indicators
            fun getBalanceDetails(bal: Double, partyType: PartyType): Triple<String, String, Int> {
                val absAmount = nf.format(Math.abs(bal))
                return when {
                    bal > 0 -> {
                        if (partyType == PartyType.BUYER) {
                            Triple("Rs. $absAmount", "⬆️ RECEIVABLE", colorSuccess)
                        } else {
                            Triple("Rs. $absAmount", "⬆️ PAYABLE", colorError)
                        }
                    }
                    bal < 0 -> {
                        if (partyType == PartyType.BUYER) {
                            Triple("Rs. $absAmount", "⬇️ PAYABLE", colorError)
                        } else {
                            Triple("Rs. $absAmount", "⬇️ RECEIVABLE", colorSuccess)
                        }
                    }
                    else -> {
                        Triple("Rs. 0", "✅ CLEAR", colorSuccess)
                    }
                }
            }

            // Box 1: Opening Balance (Previous)
            drawBoxWithShadow(20f, y, 285f, y + 85f, 10f, Color.WHITE, colorSlate300)

            paint.resetText()
            paint.color = colorSlate500
            paint.textSize = 10f
            paint.typeface = tfBold
            canvas.drawText("PREVIOUS BALANCE:", 35f, y + 20f, paint)
            paint.resetText()
            paint.textSize = 9f
            paint.typeface = tfRegular
            paint.color = colorSlate600
            canvas.drawText("(Before $startStr)", 35f, y + 33f, paint)

            val (openAmt, openLabel, openColor) =
                    getBalanceDetails(openingBalance, details.party.type)
            paint.resetText()
            paint.color = openColor
            paint.textSize = 18f
            paint.typeface = tfBold
            canvas.drawText(openAmt, 35f, y + 58f, paint)

            paint.resetText()
            paint.textSize = 9f
            paint.typeface = tfRegular
            openLabel.split("\n").forEachIndexed { idx, line ->
                canvas.drawText(line, 35f, y + 72f + (idx * 12f), paint)
            }

            // Box 2: FINAL BALANCE - MUCH BIGGER AND MORE PROMINENT
            val finalBoxColor = 0xFFFEF3C7.toInt() // Light amber background for emphasis
            drawBoxWithShadow(310f, y, 575f, y + 135f, 12f, finalBoxColor, colorWarning)

            // "FINAL BALANCE" header with icon
            paint.resetText()
            paint.color = colorSlate900
            paint.textSize = 13f
            paint.typeface = tfBold
            canvas.drawText("FINAL BALANCE:", 325f, y + 23f, paint)

            paint.resetText()
            paint.textSize = 9f
            paint.typeface = tfRegular
            paint.color = colorSlate600
            canvas.drawText("(As of $endStr)", 325f, y + 38f, paint)

            // Draw separator line
            paint.color = colorWarning
            paint.strokeWidth = 2f
            canvas.drawLine(325f, y + 45f, 560f, y + 45f, paint)

            // HUGE balance amount - 3x larger than before
            val (finalAmt, finalLabel, finalColor) = getBalanceDetails(balance, details.party.type)
            paint.resetText()
            paint.color = finalColor
            paint.textSize = 32f // VERY BIG
            paint.typeface = tfBold
            canvas.drawText(finalAmt, 325f, y + 85f, paint)

            // Status label - clear and prominent
            paint.resetText()
            paint.textSize = 12f
            paint.typeface = tfBold
            finalLabel.split("\n").forEachIndexed { idx, line ->
                canvas.drawText(line, 325f, y + 105f + (idx * 16f), paint)
            }

            y += 170f

            // --- ENHANCED TABLE with grid lines and better readability ---
            val tableHeaderHeight = 35f
            val rowHeight = 30f

            // Column positions (adjusted for better spacing)
            // Column positions and dividers for perfect table alignment
            val div1 = 115f  // After Date
            val div2 = 285f  // After Details
            val div3 = 345f  // After Qty
            val div4 = 422f  // After Bill
            val div5 = 498f  // After Paid
            
            val colDate = 25f
            val colDetails = div1 + 8f
            val colQty = div2 + 8f
            val colBill = div3 + 8f
            val colPaid = div4 + 8f
            val colBalance = div5 + 8f

            // Mid-points for horizontal centering
            val midDate = (20f + div1) / 2
            val midDetails = (div1 + div2) / 2
            val midQty = (div2 + div3) / 2
            val midBill = (div3 + div4) / 2
            val midPaid = (div4 + div5) / 2
            val midBalance = (div5 + 575f) / 2

            fun drawTableHeader(yPos: Float) {
                // Header background with gradient
                paint.color = colorSlate900
                canvas.drawRect(20f, yPos - 25f, 575f, yPos + 10f, paint)

                // Header text properties
                paint.resetText()
                paint.color = Color.WHITE
                paint.textSize = 11f
                paint.typeface = tfBold
                paint.textAlign = Paint.Align.CENTER
                
                // Draw text centered vertically by adjusting baseline
                val headerY = yPos - ((paint.descent() + paint.ascent()) / 2) - 7.5f

                canvas.drawText("Date", midDate, headerY, paint)
                canvas.drawText("Details", midDetails, headerY, paint)
                canvas.drawText("Qty (T)", midQty, headerY, paint)
                canvas.drawText("Bill", midBill, headerY, paint)
                canvas.drawText("Paid", midPaid, headerY, paint)
                canvas.drawText("Balance", midBalance, headerY, paint)
                
                paint.textAlign = Paint.Align.LEFT // Reset

                // Column separator lines
                paint.color = 0x40FFFFFF.toInt() // Semi-transparent white
                paint.strokeWidth = 1f
                canvas.drawLine(div1, yPos - 25f, div1, yPos + 10f, paint)
                canvas.drawLine(div2, yPos - 25f, div2, yPos + 10f, paint)
                canvas.drawLine(div3, yPos - 25f, div3, yPos + 10f, paint)
                canvas.drawLine(div4, yPos - 25f, div4, yPos + 10f, paint)
                canvas.drawLine(div5, yPos - 25f, div5, yPos + 10f, paint)
            }

            drawTableHeader(y)
            y += 25f

            // Pre-compute running balances in chronological order (rangeItems is reversed for display)
            // We need balance AFTER each transaction, keyed by display index
            val chronologicalItems = rangeItems.reversed() // back to chronological
            val balancesChronological = mutableListOf<Double>()
            var tempBal = openingBalance
            chronologicalItems.forEach { (item, type) ->
                if (type == "entry") {
                    val entry = item as LedgerEntry
                    val bill = ((entry.weight ?: 0.0) * (entry.rate ?: 0.0)) + (entry.fare ?: 0.0)
                    val contribution = if (details.party.type == PartyType.BUYER) bill else -bill
                    tempBal += contribution
                } else {
                    val p = item as Payment
                    val isBuyer = details.party.type == PartyType.BUYER
                    val isIncoming = p.type == PaymentType.THEY_PAID
                    val isNegativeEffect = if (isBuyer) isIncoming else !isIncoming
                    val contribution = if (isNegativeEffect) -p.amount else p.amount
                    tempBal += contribution
                }
                balancesChronological.add(tempBal)
            }
            // Reverse to match display order (newest first)
            val balancesForDisplay = balancesChronological.reversed()

            var rowIndex = 0

            rangeItems.forEachIndexed { displayIndex, (item, type) ->
                // Check for new page
                if (y > 750) {
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    drawHeader(pageNumber)
                    y = 165f
                    drawTableHeader(y)
                    y += 25f
                    rowIndex = 0
                }

                // Alternating row background with better colors
                if (rowIndex % 2 == 0) {
                    paint.color = Color.WHITE
                } else {
                    paint.color = colorSlate100
                }
                canvas.drawRect(20f, y - 22f, 575f, y + 8f, paint)

                // Grid lines for better separation
                paint.style = Paint.Style.STROKE
                paint.color = colorSlate300
                paint.strokeWidth = 0.5f
                canvas.drawLine(20f, y + 8f, 575f, y + 8f, paint)
                paint.style = Paint.Style.FILL

                rowIndex++

                paint.resetText()
                paint.color = colorSlate700
                paint.textSize = 10f
                paint.typeface = tfRegular
                paint.textAlign = Paint.Align.CENTER
                
                // Vertical center calculation for the row (height is usually from y-22 to y+8)
                val rowY = y - ((paint.descent() + paint.ascent()) / 2) - 7f

                val date = if (item is LedgerEntry) item.date else (item as Payment).date
                canvas.drawText(df.format(Date(date)), midDate, rowY, paint)

                if (type == "entry") {
                    val entry = item as LedgerEntry
                    val detailsStr = "Truck ${entry.truckNumber ?: "Entry"}${entry.mine?.let { " - $it" } ?: ""}"
                    canvas.drawText(detailsStr, midDetails, rowY, paint)

                    val qtyStr = if (entry.weight != null) "${entry.weight} tons" else "-"
                    canvas.drawText(qtyStr, midQty, rowY, paint)

                    val bill = ((entry.weight ?: 0.0) * (entry.rate ?: 0.0)) + (entry.fare ?: 0.0)
                    paint.resetText()
                    // Buyer: Bill INCREASES Receivable -> Red
                    // Supplier: Bill INCREASES Payable -> Green
                    paint.color = if (details.party.type == PartyType.BUYER) colorError else colorSuccess
                    paint.textSize = 11f
                    paint.typeface = tfBold
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(nf.format(bill), midBill, rowY, paint)

                    paint.resetText()
                    paint.color = colorSlate500
                    paint.textSize = 10f
                    paint.typeface = tfRegular
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("-", midPaid, rowY, paint)
                } else {
                    val p = item as Payment
                    val detailsStr = "Payment${p.note?.let { " - $it" } ?: ""}"
                    canvas.drawText(detailsStr, midDetails, rowY, paint)

                    canvas.drawText("-", midQty, rowY, paint)
                    canvas.drawText("-", midBill, rowY, paint)

                    val isIncoming = p.type == PaymentType.THEY_PAID
                    
                    paint.resetText()
                    // Inflow of cash -> Green, Outflow -> Red
                    paint.color = if (isIncoming) colorSuccess else colorError
                    paint.textSize = 11f
                    paint.typeface = tfBold
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(nf.format(p.amount), midPaid, rowY, paint)
                }
                
                // Use pre-computed balance for this row
                val rowBalance = balancesForDisplay[displayIndex]
                val balColor = if (rowBalance > 0) {
                    if (details.party.type == PartyType.BUYER) colorSuccess else colorError
                } else if (rowBalance < 0) {
                    if (details.party.type == PartyType.BUYER) colorError else colorSuccess
                } else colorSuccess
                paint.resetText()
                paint.color = balColor
                paint.textSize = 11f
                paint.typeface = tfBold
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(nf.format(Math.abs(rowBalance)), midBalance, rowY, paint)

                paint.textAlign = Paint.Align.LEFT // Reset for next operations

                // Draw vertical grid lines for each column
                paint.style = Paint.Style.STROKE
                paint.color = colorSlate300
                paint.strokeWidth = 0.5f
                canvas.drawLine(div1, y - 22f, div1, y + 8f, paint)
                canvas.drawLine(div2, y - 22f, div2, y + 8f, paint)
                canvas.drawLine(div3, y - 22f, div3, y + 8f, paint)
                canvas.drawLine(div4, y - 22f, div4, y + 8f, paint)
                canvas.drawLine(div5, y - 22f, div5, y + 8f, paint)
                paint.style = Paint.Style.FILL

                y += rowHeight
            }

            // Table bottom border
            // paint.style = Paint.Style.STROKE
            // paint.color = colorSlate900
            // paint.strokeWidth = 2f
            // canvas.drawLine(20f, y - 22f, 575f, y - 22f, paint)
            // paint.style = Paint.Style.FILL

            // --- FOOTER SECTION ---
            if (y > 680) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(pageNumber)
                y = 165f
            }

            y += 50f

            // Add helpful note for understanding
            paint.resetText()
            paint.color = colorSlate500
            paint.textSize = 9f
            paint.typeface = tfItalic
            canvas.drawText(
                    "Thank You for conducting business with us.",
                    25f,
                    y,
                    paint
            )

            y += 35f

            // --- SIGNATURE SECTION with better styling ---
            if (signatureBitmap != null) {
                // Signature box
                paint.color = Color.WHITE
                canvas.drawRoundRect(420f, y - 10f, 565f, y + 75f, 8f, 8f, paint)
                paint.style = Paint.Style.STROKE
                paint.color = colorSlate300
                paint.strokeWidth = 1f
                canvas.drawRoundRect(420f, y - 10f, 565f, y + 75f, 8f, 8f, paint)
                paint.style = Paint.Style.FILL

                val dst = RectF(430f, y, 555f, y + 35f)
                canvas.drawBitmap(signatureBitmap, null, dst, paint)
            }

            paint.color = colorSlate700
            paint.strokeWidth = 1.5f
            canvas.drawLine(420f, y + 48f, 565f, y + 48f, paint)

            paint.resetText()
            paint.textSize = 10f
            paint.typeface = tfBold
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(ownerName.uppercase(), 492f, y + 62f, paint)

            //paint.textSize = 8f
            //paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            //paint.color = colorSlate500
            //canvas.drawText("", 492f, y + 74f, paint)
            //paint.textAlign = Paint.Align.LEFT

            // --- FINISH ---
            pdfDocument.finishPage(currentPage)

            val safeName = details.party.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val file =
                    File(
                            context.cacheDir,
                            "${safeName}_Statement_${System.currentTimeMillis()}.pdf"
                    )
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()

            Toast.makeText(context, "✅ PDF Generated Successfully!", Toast.LENGTH_SHORT).show()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "❌ PDF generation failed: ${e.message}", Toast.LENGTH_LONG)
                    .show()
        }
    }

    // === KEEP ALL OTHER FUNCTIONS UNCHANGED ===

    fun shareBackup(context: Context, json: String) {
        try {
            val fileName = "AwanCoalLedger_Backup_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { out -> out.write(json.toByteArray()) }

            val uri: Uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_SUBJECT, "Awan Coal Ledger Backup")
                        putExtra(Intent.EXTRA_TEXT, "Here is your Ledger Data Backup file.")
                    }

            context.startActivity(Intent.createChooser(intent, "Share Backup File"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share backup: ${e.message}", Toast.LENGTH_LONG)
                    .show()
        }
    }

    fun generateAndShareExcel(
            context: Context,
            details: PartyWithDetails,
            startDate: Long? = null,
            endDate: Long? = null
    ) {
        try {
            val df = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val nf = DecimalFormat("#,###.##")

            // Sort all transactions; split into opening-balance items and in-range items
            val allTx = (details.entries.map { it to "entry" } + details.payments.map { it to "payment" })
                .sortedBy { if (it.first is LedgerEntry) (it.first as LedgerEntry).date else (it.first as Payment).date }

            var openingBalance = 0.0
            val rangeItems = mutableListOf<Pair<Any, String>>()
            allTx.forEach { (item, type) ->
                val date   = if (item is LedgerEntry) item.date else (item as Payment).date
                val amount = if (type == "entry") { 
                                 val e = item as LedgerEntry; 
                                 ((e.weight ?: 0.0) * (e.rate ?: 0.0)) + (e.fare ?: 0.0) 
                             } else (item as Payment).amount
                val isBuyer = details.party.type == PartyType.BUYER
                val contrib = if (type == "entry") {
                    if (isBuyer) amount else -amount
                } else {
                    val isIncoming = (item as Payment).type == PaymentType.THEY_PAID
                    val isNegative = if (isBuyer) isIncoming else !isIncoming
                    if (isNegative) -amount else amount
                }
                if (startDate != null && date < startDate) openingBalance += contrib
                else if (endDate == null || date <= endDate) rangeItems.add(item to type)
            }

            val headers = listOf("Date", "Type", "Truck / Note", "Weight (T)", "Mine", "Amount (Rs)", "Paid (Rs)", "Balance (Rs)")

            val dataRows = mutableListOf<List<Pair<String, Int>>>()
            var runningBal = openingBalance
            rangeItems.forEach { (item, type) ->
                if (type == "entry") {
                    val e    = item as LedgerEntry
                    val coal = (e.weight ?: 0.0) * (e.rate ?: 0.0)
                    val bill = coal + (e.fare ?: 0.0)
                    runningBal += if (details.party.type == PartyType.BUYER) bill else -bill
                    
                    val colorStyle = if (details.party.type == PartyType.BUYER) XlsxWriter.TYPE_RED else XlsxWriter.TYPE_GREEN
                    
                    dataRows.add(listOf(
                        Pair(df.format(Date(e.date)), XlsxWriter.TYPE_NORMAL),
                        Pair("COAL", XlsxWriter.TYPE_NORMAL),
                        Pair(e.truckNumber ?: "-", XlsxWriter.TYPE_NORMAL),
                        Pair("${e.weight ?: 0.0}", XlsxWriter.TYPE_NORMAL),
                        Pair(e.mine ?: "-", XlsxWriter.TYPE_NORMAL),
                        Pair(nf.format(bill), colorStyle),
                        Pair("-", XlsxWriter.TYPE_NORMAL),
                        Pair(nf.format(runningBal), XlsxWriter.TYPE_NORMAL)
                    ))
                } else {
                    val p    = item as Payment
                    runningBal += if (details.party.type == PartyType.BUYER) -p.amount else p.amount
                    val note = p.note ?: if (p.type == PaymentType.THEY_PAID) "Received" else "Paid"
                    
                    val isIncoming = p.type == PaymentType.THEY_PAID
                    val colorStyle = if (isIncoming) XlsxWriter.TYPE_GREEN else XlsxWriter.TYPE_RED
                    
                    dataRows.add(listOf(
                        Pair(df.format(Date(p.date)), XlsxWriter.TYPE_NORMAL),
                        Pair("PAYMENT", XlsxWriter.TYPE_NORMAL),
                        Pair(note, XlsxWriter.TYPE_NORMAL),
                        Pair("-", XlsxWriter.TYPE_NORMAL),
                        Pair("-", XlsxWriter.TYPE_NORMAL),
                        Pair("-", XlsxWriter.TYPE_NORMAL),
                        Pair(nf.format(p.amount), colorStyle),
                        Pair(nf.format(runningBal), XlsxWriter.TYPE_NORMAL)
                    ))
                }
            }

            val startStr  = if (startDate != null) df.format(Date(startDate)) else "Beginning"
            val endStr    = if (endDate   != null) df.format(Date(endDate))   else df.format(Date())
            val partyLine = "${details.party.name}  |  ${details.party.phone ?: ""}  |  ${details.party.address ?: ""}"

            val summary = listOf(
                "Opening Balance" to nf.format(openingBalance),
                "Statement Period" to "$startStr → $endStr",
                "FINAL BALANCE" to "Rs. ${nf.format(runningBal)}"
            )

            val bytes = XlsxWriter.create(
                title    = "AWAN COAL LEDGER",
                subtitle = partyLine,
                headers  = headers,
                dataRows = dataRows,
                summary  = summary
            )

            val safeName = details.party.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val file = File(context.cacheDir, "${safeName}_Ledger_${System.currentTimeMillis()}.xlsx")
            file.writeBytes(bytes)

            Toast.makeText(context, "\u2705 Excel Generated!", Toast.LENGTH_SHORT).show()
            shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "\u274C Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareDetailedEntry(context: Context, entry: LedgerEntry) {
        val df = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val weight = entry.weight ?: 0.0
        val ratePerTon = entry.rate ?: 0.0
        val coalAmount = weight * ratePerTon
        val fareAmount = entry.fare ?: 0.0
        val total = coalAmount + fareAmount

        val nf = DecimalFormat("#,###.##")
        val text =
                """
            📦 *COAL TRANSACTION DETAILS*
            -----------------------------------
            📅 *Date:* ${df.format(Date(entry.date))}
            🚛 *Truck #:* ${entry.truckNumber ?: "-"}
            📍 *Mine:* ${entry.mine ?: "-"}
            🏢 *Warehouse:* ${entry.warehouse ?: "-"}
            ⚖️ *Weight:* ${entry.weight ?: 0.0} Tons
            
            💰 *Coal Amount:* Rs. ${nf.format(coalAmount)}
            🚚 *Fare / Freight:* Rs. ${nf.format(fareAmount)}
            
            -----------------------------------
            💵 *NET TOTAL:* Rs. ${nf.format(total)}
            -----------------------------------
            *AWAN COAL LEDGER*
        """.trimIndent()
        shareText(context, text, "com.whatsapp")
    }

    fun shareDetailedPayment(context: Context, payment: Payment) {
        val df = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val type = if (payment.type == PaymentType.THEY_PAID) "Cash Received" else "Payment Sent"
        val text =
                """
            💸 *PAYMENT SLIP*
            -----------------------------------
            📅 *Date:* ${df.format(Date(payment.date))}
            📝 *Type:* $type
            💰 *Amount:* Rs. ${String.format(Locale.getDefault(), "%,.0f", payment.amount)}
            🗒️ *Note:* ${payment.note ?: "-"}
            -----------------------------------
            *AWAN COAL LEDGER*
        """.trimIndent()
        shareText(context, text, "com.whatsapp")
    }

    fun shareText(context: Context, text: String, targetPackage: String? = null) {
        try {
            val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        if (targetPackage != null) {
                            setPackage(targetPackage)
                        }
                    }

            if (targetPackage != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, "Share Entry"))
            }
        } catch (e: Exception) {
            if (targetPackage == "com.whatsapp") {
                val fallbackIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                context.startActivity(Intent.createChooser(fallbackIntent, "Share via..."))
            } else {
                Toast.makeText(context, "Failed to share text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateExpensesPdf(
        context: Context,
        expenses: List<Expense>,
        title: String,
        businessName: String,
        ownerName: String,
        logoUri: String? = null,
        signatureUri: String? = null
    ) {
        try {
            val pdfDocument = PdfDocument()
            val df = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val nf = DecimalFormat("#,###")

            val colorSlate900 = 0xFF0F172A.toInt()
            val colorSlate700 = 0xFF334155.toInt()
            val colorSlate500 = 0xFF64748B.toInt()
            val colorSlate300 = 0xFFCBD5E1.toInt()
            val colorSlate100 = 0xFFF1F5F9.toInt()
            val colorHeaderBg = 0xFFF8FAFC.toInt()
            val colorBlue = 0xFF3B82F6.toInt()

            fun loadBitmap(uriString: String?): Bitmap? {
                if (uriString == null) return null
                return try {
                    val uri = Uri.parse(uriString)
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val logoBitmap = loadBitmap(logoUri)
            val signatureBitmap = loadBitmap(signatureUri)

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas
            val paint = Paint().apply {
                isAntiAlias = true
                isSubpixelText = true
                hinting = Paint.HINTING_ON
            }

            // Helper to reset paint text state before every drawText call
            fun Paint.resetText() {
                letterSpacing = 0f
                textScaleX = 1f
            }

            fun drawHeader(pageNum: Int) {
                paint.color = colorHeaderBg
                canvas.drawRect(0f, 0f, 595f, 140f, paint)
                paint.color = colorBlue
                canvas.drawRect(0f, 0f, 595f, 8f, paint)

                if (logoBitmap != null) {
                    val dst = RectF(25f, 30f, 95f, 100f)
                    paint.color = Color.WHITE
                    canvas.drawRoundRect(23f, 28f, 97f, 102f, 12f, 12f, paint)
                    canvas.drawBitmap(logoBitmap, null, dst, paint)
                } else {
                    paint.color = 0xFFE0E7FF.toInt()
                    canvas.drawRoundRect(25f, 30f, 95f, 100f, 12f, 12f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = colorBlue
                    canvas.drawRoundRect(25f, 30f, 95f, 100f, 12f, 12f, paint)
                    paint.style = Paint.Style.FILL
                }

                paint.resetText()
                paint.color = colorSlate900
                paint.textSize = 24f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(businessName, if (logoBitmap != null) 110f else 25f, 55f, paint)

                paint.resetText()
                paint.color = colorSlate500
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("$ownerName | Expense Report", if (logoBitmap != null) 110f else 25f, 75f, paint)

                paint.resetText()
                paint.textAlign = Paint.Align.RIGHT
                paint.color = colorSlate900
                paint.textSize = 15f
                canvas.drawText(title.uppercase(), 570f, 60f, paint)
                paint.resetText()
                paint.color = colorSlate500
                paint.textSize = 10f
                canvas.drawText("Printed: ${df.format(Date())}", 570f, 80f, paint)
                canvas.drawText("Page $pageNum", 570f, 100f, paint)
                paint.textAlign = Paint.Align.LEFT

                paint.color = colorSlate300
                paint.strokeWidth = 1f
                canvas.drawLine(0f, 140f, 595f, 140f, paint)
            }

            drawHeader(pageNumber)
            var y = 180f

            // Summary Box
            val totalAmount = expenses.sumOf { it.amount }
            paint.color = colorSlate100
            canvas.drawRoundRect(20f, y, 575f, y + 60f, 12f, 12f, paint)
            paint.resetText()
            paint.color = colorSlate900
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOTAL EXPENSES", 40f, y + 25f, paint)
            paint.textSize = 24f
            paint.color = 0xFFDC2626.toInt() // Expense Red
            canvas.drawText("Rs. ${nf.format(totalAmount)}", 40f, y + 52f, paint)
            
            y += 90f

            // Table Header
            paint.color = colorSlate900
            canvas.drawRect(20f, y, 575f, y + 30f, paint)
            paint.color = Color.WHITE
            paint.resetText()
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Date", 35f, y + 20f, paint)
            canvas.drawText("Category", 140f, y + 20f, paint)
            canvas.drawText("Note", 260f, y + 20f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount", 560f, y + 20f, paint)
            paint.textAlign = Paint.Align.LEFT

            y += 30f
            val rowHeight = 25f
            expenses.sortedByDescending { it.date }.forEachIndexed { index, expense ->
                if (y > 750) {
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    drawHeader(pageNumber)
                    y = 160f
                }

                if (index % 2 != 0) {
                    paint.color = colorSlate100
                    canvas.drawRect(20f, y, 575f, y + rowHeight, paint)
                }

                paint.resetText()
                paint.color = colorSlate700
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date)), 35f, y + 17f, paint)
                canvas.drawText(expense.category.name, 140f, y + 17f, paint)
                canvas.drawText(expense.note ?: "-", 260f, y + 17f, paint)
                paint.resetText()
                paint.textAlign = Paint.Align.RIGHT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(nf.format(expense.amount), 560f, y + 17f, paint)
                paint.textAlign = Paint.Align.LEFT
                
                y += rowHeight
            }

            // Signature
            if (y > 700) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(pageNumber)
                y = 160f
            }
            
            y += 40f
            if (signatureBitmap != null) {
                val dst = RectF(430f, y, 555f, y + 40f)
                canvas.drawBitmap(signatureBitmap, null, dst, paint)
            }
            paint.color = colorSlate700
            canvas.drawLine(420f, y + 45f, 565f, y + 45f, paint)
            paint.textAlign = Paint.Align.CENTER
            paint.resetText()
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(ownerName.uppercase(), 492f, y + 60f, paint)

            pdfDocument.finishPage(currentPage)
            val file = File(context.cacheDir, "Expenses_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        if (!file.exists()) return
        val authority = "${context.packageName}.provider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
        context.startActivity(Intent.createChooser(intent, "Share Statement"))
    }
}
