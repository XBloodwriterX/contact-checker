package com.example.contactchecker.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat {
    CSV,
    PLAIN_TEXT
}

enum class ExportFilter {
    VALID_ONLY,
    INVALID_ONLY,
    PENDING_ONLY,
    VERIFIED_ONLY,
    ALL
}

enum class ExportPackaging {
    SINGLE_FILE,
    SEPARATE_FILES
}

object ContactExporter {

    private fun getDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    fun filterContacts(contacts: List<ContactItem>, filter: ExportFilter): List<ContactItem> {
        return when (filter) {
            ExportFilter.VALID_ONLY -> contacts.filter { it.status == ContactStatus.VALID }
            ExportFilter.INVALID_ONLY -> contacts.filter { it.status == ContactStatus.INVALID }
            ExportFilter.PENDING_ONLY -> contacts.filter { it.status == ContactStatus.PENDING }
            ExportFilter.VERIFIED_ONLY -> contacts.filter { (it.status == ContactStatus.VALID) || (it.status == ContactStatus.INVALID) }
            ExportFilter.ALL -> contacts
        }
    }

    fun exportToPlainText(
        contacts: List<ContactItem>,
        filter: ExportFilter = ExportFilter.ALL
    ): String {
        val filtered = filterContacts(contacts, filter)
        return filtered.joinToString("\n") { it.phoneNumber }
    }

    fun exportToPlainText(
        contacts: List<ContactItem>,
        validOnly: Boolean
    ): String {
        val filter = if (validOnly) ExportFilter.VALID_ONLY else ExportFilter.ALL
        return exportToPlainText(contacts, filter)
    }

    fun exportToCsv(
        contacts: List<ContactItem>,
        filter: ExportFilter = ExportFilter.ALL
    ): String {
        val filtered = filterContacts(contacts, filter)

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

    fun exportToCsv(
        contacts: List<ContactItem>,
        validOnly: Boolean
    ): String {
        val filter = if (validOnly) ExportFilter.VALID_ONLY else ExportFilter.ALL
        return exportToCsv(contacts, filter)
    }

    fun exportDataByCategories(
        contacts: List<ContactItem>,
        filter: ExportFilter = ExportFilter.ALL,
        format: ExportFormat = ExportFormat.CSV
    ): Map<ContactStatus, String> {
        val filtered = filterContacts(contacts, filter)
        val grouped = filtered.groupBy { it.status }
        val sortedKeys = grouped.keys.sortedBy { statusPriority(it) }
        return sortedKeys.associateWith { status ->
            val items = grouped[status] ?: emptyList()
            when (format) {
                ExportFormat.CSV -> exportToCsv(items, ExportFilter.ALL)
                ExportFormat.PLAIN_TEXT -> exportToPlainText(items, ExportFilter.ALL)
            }
        }
    }

    private fun statusPriority(status: ContactStatus): Int {
        return when (status) {
            ContactStatus.VALID -> 0
            ContactStatus.INVALID -> 1
            ContactStatus.PENDING -> 2
            ContactStatus.IN_PROGRESS -> 3
            ContactStatus.SKIPPED -> 4
        }
    }

    fun getFileNameForStatus(status: ContactStatus, format: ExportFormat): String {
        val ext = if (format == ExportFormat.CSV) "csv" else "txt"
        val prefix = when (status) {
            ContactStatus.VALID -> "valid"
            ContactStatus.INVALID -> "invalid"
            ContactStatus.PENDING -> "pending"
            ContactStatus.IN_PROGRESS -> "in_progress"
            ContactStatus.SKIPPED -> "skipped"
        }
        return "${prefix}_contacts.$ext"
    }

    fun getCategoryHeader(status: ContactStatus): String {
        val name = when (status) {
            ContactStatus.VALID -> "VALID"
            ContactStatus.INVALID -> "INVALID"
            ContactStatus.PENDING -> "PENDING"
            ContactStatus.IN_PROGRESS -> "IN PROGRESS"
            ContactStatus.SKIPPED -> "SKIPPED"
        }
        return "=== $name CONTACTS ==="
    }

    fun exportData(
        contacts: List<ContactItem>,
        filter: ExportFilter = ExportFilter.ALL,
        format: ExportFormat = ExportFormat.CSV,
        packaging: ExportPackaging = ExportPackaging.SINGLE_FILE
    ): String {
        return when (packaging) {
            ExportPackaging.SINGLE_FILE -> {
                when (format) {
                    ExportFormat.CSV -> exportToCsv(contacts, filter)
                    ExportFormat.PLAIN_TEXT -> exportToPlainText(contacts, filter)
                }
            }
            ExportPackaging.SEPARATE_FILES -> {
                val categoriesMap = exportDataByCategories(contacts, filter, format)
                if (categoriesMap.isEmpty()) {
                    ""
                } else {
                    categoriesMap.entries.joinToString("\n\n") { (status, content) ->
                        "${getCategoryHeader(status)}\n$content"
                    }
                }
            }
        }
    }

    fun copyToClipboard(
        context: Context,
        contacts: List<ContactItem>,
        filter: ExportFilter = ExportFilter.ALL,
        format: ExportFormat = ExportFormat.CSV,
        packaging: ExportPackaging = ExportPackaging.SINGLE_FILE,
        label: String = "Exported Contacts"
    ) {
        val content = exportData(contacts, filter, format, packaging)
        copyToClipboard(context, content, label)
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

    fun shareExport(
        context: Context,
        contacts: List<ContactItem>,
        filter: ExportFilter = ExportFilter.ALL,
        format: ExportFormat = ExportFormat.CSV,
        packaging: ExportPackaging = ExportPackaging.SINGLE_FILE,
        title: String = "Share Contacts"
    ) {
        val filtered = filterContacts(contacts, filter)
        if (filtered.isEmpty()) {
            Toast.makeText(context, "No contacts to export", Toast.LENGTH_SHORT).show()
            return
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val mimeType = when (format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.PLAIN_TEXT -> "text/plain"
        }
        val authority = "${context.packageName}.fileprovider"

        if (packaging == ExportPackaging.SINGLE_FILE) {
            val content = exportData(contacts, filter, format, ExportPackaging.SINGLE_FILE)
            val fileName = if (format == ExportFormat.CSV) "contacts_export.csv" else "contacts_export.txt"
            val file = File(exportDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(context, authority, file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Contact Checker Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } else {
            val categoriesMap = exportDataByCategories(contacts, filter, format)
            if (categoriesMap.isEmpty()) {
                Toast.makeText(context, "No contacts to export", Toast.LENGTH_SHORT).show()
                return
            }

            val fileUris = ArrayList<Uri>()
            for ((status, categoryContent) in categoriesMap) {
                val fileName = getFileNameForStatus(status, format)
                val file = File(exportDir, fileName)
                file.writeText(categoryContent)
                val uri = FileProvider.getUriForFile(context, authority, file)
                fileUris.add(uri)
            }

            if (fileUris.size == 1) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, fileUris[0])
                    putExtra(Intent.EXTRA_SUBJECT, "Contact Checker Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val shareIntent = Intent.createChooser(sendIntent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(shareIntent)
            } else {
                val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = mimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                    putExtra(Intent.EXTRA_SUBJECT, "Contact Checker Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val shareIntent = Intent.createChooser(sendIntent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(shareIntent)
            }
        }
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
