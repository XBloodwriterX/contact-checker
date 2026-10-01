package com.example.contactchecker.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat {
    CSV,
    PLAIN_TEXT
}

enum class ExportFilter {
    VALID_ONLY,
    ALL
}

object ContactExporter {

    private fun getDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    fun exportToPlainText(
        contacts: List<ContactItem>,
        validOnly: Boolean = false
    ): String {
        val filtered = if (validOnly) {
            contacts.filter { it.status == ContactStatus.VALID }
        } else {
            contacts
        }
        return filtered.joinToString("\n") { it.phoneNumber }
    }

    fun exportToCsv(
        contacts: List<ContactItem>,
        validOnly: Boolean = false
    ): String {
        val filtered = if (validOnly) {
            contacts.filter { it.status == ContactStatus.VALID }
        } else {
            contacts
        }

        val dateFormat = getDateFormat()
        val header = "Phone Number,Status,Verification Note,Timestamp"
        val rows = filtered.map { item ->
            val formattedDate = try {
                dateFormat.format(Date(item.timestamp))
            } catch (_: Exception) {
                item.timestamp.toString()
            }
            "${escapeCsv(item.phoneNumber)},${escapeCsv(item.status.name)},${escapeCsv(item.note ?: "")},${escapeCsv(formattedDate)}"
        }

        return (listOf(header) + rows).joinToString("\n")
    }

    fun exportData(
        contacts: List<ContactItem>,
        filter: ExportFilter,
        format: ExportFormat
    ): String {
        val validOnly = filter == ExportFilter.VALID_ONLY
        return when (format) {
            ExportFormat.CSV -> exportToCsv(contacts, validOnly)
            ExportFormat.PLAIN_TEXT -> exportToPlainText(contacts, validOnly)
        }
    }

    fun copyToClipboard(
        context: Context,
        content: String,
        label: String = "Exported Contacts"
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareContent(
        context: Context,
        content: String,
        format: ExportFormat,
        title: String = "Share Contacts"
    ) {
        val mimeType = when (format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.PLAIN_TEXT -> "text/plain"
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, "Contact Checker Export")
        }

        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(shareIntent)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
