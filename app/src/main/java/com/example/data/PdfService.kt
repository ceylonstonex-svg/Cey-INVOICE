package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfService {

    fun generateInvoicePdf(context: Context, invoiceWithItems: InvoiceWithLineItems): File {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.lineItems

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Set up paints
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0C2417") // ForestDark
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#7CA990") // ForestSage
            textSize = 12f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#1B4D3E") // ForestPrimary
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#DFD9CE") // ForestDivider
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val fillPaint = Paint().apply {
            color = Color.parseColor("#FAF6EE") // IvoryTrue background fill
            style = Paint.Style.FILL
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun formatMoney(amount: Double): String {
            return "${invoice.currencySymbol} " + String.format(Locale.US, "%,.2f", amount)
        }

        // 1. Draw Title Header
        canvas.drawText("INVOICE", 40f, 55f, titlePaint)
        canvas.drawText("Invoice #: ${invoice.invoiceNumber}", 40f, 75f, subTitlePaint)
        canvas.drawText("Date: ${dateFormat.format(Date(invoice.invoiceDate))}", 330f, 55f, textPaint)
        canvas.drawText("Due Date: ${dateFormat.format(Date(invoice.dueDate))}", 330f, 70f, textPaint)

        // Draw Logo at top right
        try {
            val logoBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.img_ceyvana_logo)
            if (logoBitmap != null) {
                val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoBitmap, 75, 75, true)
                canvas.drawBitmap(scaledLogo, 480f, 20f, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Horizontal Rule
        canvas.drawLine(40f, 100f, 555f, 100f, linePaint)

        // 2. Billing info
        canvas.drawText("BILL TO", 40f, 130f, headerPaint)
        canvas.drawText(invoice.customerName, 40f, 150f, boldPaint)
        canvas.drawText("Email: ${invoice.customerEmail}", 40f, 165f, textPaint)
        canvas.drawText("Phone: ${invoice.customerPhone}", 40f, 180f, textPaint)

        canvas.drawText("CURRENCY", 420f, 130f, headerPaint)
        canvas.drawText("${invoice.currencyCode} (${invoice.currencySymbol})", 420f, 150f, boldPaint)

        // Horizontal Rule
        canvas.drawLine(40f, 205f, 555f, 205f, linePaint)

        // 3. Line Items Table Headers
        val tableY = 230f
        // Draw header fill background
        canvas.drawRect(40f, tableY, 555f, tableY + 20f, fillPaint)
        canvas.drawRect(40f, tableY, 555f, tableY + 20f, linePaint)

        canvas.drawText("Description", 45f, tableY + 14f, headerPaint)
        canvas.drawText("Qty", 290f, tableY + 14f, headerPaint)
        canvas.drawText("Unit Price", 360f, tableY + 14f, headerPaint)
        canvas.drawText("Total", 470f, tableY + 14f, headerPaint)

        // 4. Table Body Rows
        var currentY = tableY + 20f
        for (item in items) {
            // Row divider
            canvas.drawRect(40f, currentY, 555f, currentY + 22f, linePaint)
            // Draw data
            canvas.drawText(item.description, 45f, currentY + 15f, textPaint)
            canvas.drawText("${item.quantity} ${item.unit}", 290f, currentY + 15f, textPaint)
            canvas.drawText(formatMoney(item.unitPrice), 360f, currentY + 15f, textPaint)
            canvas.drawText(formatMoney(item.quantity * item.unitPrice), 470f, currentY + 15f, textPaint)

            currentY += 22f
        }

        // 5. Financial Summary Blocks
        currentY += 15f
        val summaryX = 340f
        canvas.drawText("Subtotal:", summaryX, currentY, textPaint)
        canvas.drawText(formatMoney(invoice.subTotal), 470f, currentY, textPaint)

        currentY += 18f
        canvas.drawText("Discount:", summaryX, currentY, textPaint)
        canvas.drawText("- " + formatMoney(invoice.discount), 470f, currentY, textPaint)

        currentY += 18f
        canvas.drawText("Delivery:", summaryX, currentY, textPaint)
        canvas.drawText(formatMoney(invoice.deliveryCharge), 470f, currentY, textPaint)

        currentY += 18f
        val taxPct = String.format(Locale.US, "%.0f%%", invoice.taxRate * 100.0)
        canvas.drawText("Tax ($taxPct):", summaryX, currentY, textPaint)
        canvas.drawText(formatMoney(invoiceWithItems.taxAmount), 470f, currentY, textPaint)

        // Grand Total Row with accent box
        currentY += 10f
        canvas.drawRect(330f, currentY, 555f, currentY + 30f, fillPaint)
        canvas.drawRect(330f, currentY, 555f, currentY + 30f, linePaint)

        val totalHeaderPaint = Paint(headerPaint).apply {
            textSize = 12f
            color = Color.parseColor("#0C2417") // ForestDark
        }
        val totalValuePaint = Paint(titlePaint).apply {
            textSize = 12f
            color = Color.parseColor("#1B4D3E") // ForestPrimary for Grand Total
        }
        canvas.drawText("Grand Total:", 340f, currentY + 19f, totalHeaderPaint)
        canvas.drawText(formatMoney(invoiceWithItems.grandTotal), 470f, currentY + 19f, totalValuePaint)

        // Footer note
        canvas.drawText("Thank you for your business!", 40f, 790f, subTitlePaint)
        canvas.drawText("Page 1 of 1", 490f, 790f, subTitlePaint)

        pdfDocument.finishPage(page)

        // Save file to cache directory
        val cacheFile = File(context.cacheDir, "INV_${invoice.invoiceNumber}.pdf")
        FileOutputStream(cacheFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return cacheFile
    }
}
