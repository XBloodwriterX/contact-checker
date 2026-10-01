package com.example.contactchecker

import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import com.example.contactchecker.utils.ContactExporter
import com.example.contactchecker.utils.ExportFilter
import com.example.contactchecker.utils.ExportFormat
import com.example.contactchecker.utils.ExportPackaging
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactExporterTest {

    private val sampleContacts = listOf(
        ContactItem(
            id = "1",
            rawInput = "+18005550199",
            phoneNumber = "+18005550199",
            status = ContactStatus.VALID,
            note = "Ringing tone detected",
            timestamp = 1600000000000L
        ),
        ContactItem(
            id = "2",
            rawInput = "555-0198",
            phoneNumber = "5550198",
            status = ContactStatus.INVALID,
            note = "Number disconnected, no ring",
            timestamp = 1600000001000L
        ),
        ContactItem(
            id = "3",
            rawInput = "+442079460912",
            phoneNumber = "+442079460912",
            status = ContactStatus.VALID,
            note = "Ringing tone detected",
            timestamp = 1600000002000L
        ),
        ContactItem(
            id = "4",
            rawInput = "+1234567890",
            phoneNumber = "+1234567890",
            status = ContactStatus.PENDING,
            note = null,
            timestamp = 1600000003000L
        )
    )

    @Test
    fun testFilterContacts_ValidOnly() {
        val filtered = ContactExporter.filterContacts(sampleContacts, ExportFilter.VALID_ONLY)
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.status == ContactStatus.VALID })
    }

    @Test
    fun testFilterContacts_InvalidOnly() {
        val filtered = ContactExporter.filterContacts(sampleContacts, ExportFilter.INVALID_ONLY)
        assertEquals(1, filtered.size)
        assertEquals("5550198", filtered[0].phoneNumber)
        assertEquals(ContactStatus.INVALID, filtered[0].status)
    }

    @Test
    fun testFilterContacts_PendingOnly() {
        val filtered = ContactExporter.filterContacts(sampleContacts, ExportFilter.PENDING_ONLY)
        assertEquals(1, filtered.size)
        assertEquals("+1234567890", filtered[0].phoneNumber)
        assertEquals(ContactStatus.PENDING, filtered[0].status)
    }

    @Test
    fun testFilterContacts_VerifiedOnly() {
        val filtered = ContactExporter.filterContacts(sampleContacts, ExportFilter.VERIFIED_ONLY)
        assertEquals(3, filtered.size)
        assertTrue(filtered.none { it.status == ContactStatus.PENDING })
        assertTrue(filtered.any { it.status == ContactStatus.VALID })
        assertTrue(filtered.any { it.status == ContactStatus.INVALID })
    }

    @Test
    fun testFilterContacts_All() {
        val filtered = ContactExporter.filterContacts(sampleContacts, ExportFilter.ALL)
        assertEquals(4, filtered.size)
    }

    @Test
    fun testExportToPlainText_AllContacts() {
        val plainText = ContactExporter.exportToPlainText(sampleContacts, filter = ExportFilter.ALL)
        val lines = plainText.split("\n")

        assertEquals(4, lines.size)
        assertEquals("+18005550199", lines[0])
        assertEquals("5550198", lines[1])
        assertEquals("+442079460912", lines[2])
        assertEquals("+1234567890", lines[3])
    }

    @Test
    fun testExportToPlainText_ValidOnly() {
        val plainText = ContactExporter.exportToPlainText(sampleContacts, filter = ExportFilter.VALID_ONLY)
        val lines = plainText.split("\n")

        assertEquals(2, lines.size)
        assertEquals("+18005550199", lines[0])
        assertEquals("+442079460912", lines[1])
        assertFalse(plainText.contains("5550198"))
    }

    @Test
    fun testExportToCsv_AllContacts() {
        val csv = ContactExporter.exportToCsv(sampleContacts, filter = ExportFilter.ALL)
        val lines = csv.split("\n")

        assertEquals(5, lines.size) // Header + 4 rows
        assertEquals("Phone Number,Status,Verification Note,Timestamp", lines[0])

        assertTrue(lines[1].startsWith("+18005550199,VALID,Ringing tone detected"))
        assertTrue(lines[2].contains("INVALID"))
        assertTrue(lines[2].contains("5550198"))
        assertTrue(lines[2].contains("\"Number disconnected, no ring\""))
        assertTrue(lines[3].startsWith("+442079460912,VALID,Ringing tone detected"))
        assertTrue(lines[4].contains("+1234567890,PENDING"))
    }

    @Test
    fun testExportToCsv_ValidOnly() {
        val csv = ContactExporter.exportToCsv(sampleContacts, filter = ExportFilter.VALID_ONLY)
        val lines = csv.split("\n")

        assertEquals(3, lines.size) // Header + 2 rows
        assertEquals("Phone Number,Status,Verification Note,Timestamp", lines[0])
        assertFalse(csv.contains("5550198"))
        assertFalse(csv.contains("INVALID"))
        assertFalse(csv.contains("PENDING"))
    }

    @Test
    fun testExportToCsv_EscapingSpecialChars() {
        val specialContacts = listOf(
            ContactItem(
                id = "1",
                rawInput = "123",
                phoneNumber = "123",
                status = ContactStatus.VALID,
                note = "Note with \"quotes\" and , comma"
            )
        )

        val csv = ContactExporter.exportToCsv(specialContacts)
        val lines = csv.split("\n")

        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("\"Note with \"\"quotes\"\" and , comma\""))
    }

    @Test
    fun testExportData_SingleFilePackaging() {
        val csvResult = ContactExporter.exportData(
            sampleContacts,
            filter = ExportFilter.VERIFIED_ONLY,
            format = ExportFormat.CSV,
            packaging = ExportPackaging.SINGLE_FILE
        )
        val lines = csvResult.split("\n")
        assertEquals(4, lines.size) // Header + 3 verified rows
        assertFalse(csvResult.contains("PENDING"))

        val textResult = ContactExporter.exportData(
            sampleContacts,
            filter = ExportFilter.ALL,
            format = ExportFormat.PLAIN_TEXT,
            packaging = ExportPackaging.SINGLE_FILE
        )
        val textLines = textResult.split("\n")
        assertEquals(4, textLines.size)
    }

    @Test
    fun testExportData_SeparateFilesPackaging() {
        val separateText = ContactExporter.exportData(
            sampleContacts,
            filter = ExportFilter.ALL,
            format = ExportFormat.PLAIN_TEXT,
            packaging = ExportPackaging.SEPARATE_FILES
        )

        assertTrue(separateText.contains("=== VALID CONTACTS ==="))
        assertTrue(separateText.contains("=== INVALID CONTACTS ==="))
        assertTrue(separateText.contains("=== PENDING CONTACTS ==="))
        assertTrue(separateText.contains("+18005550199"))
        assertTrue(separateText.contains("5550198"))
        assertTrue(separateText.contains("+1234567890"))

        val separateCsv = ContactExporter.exportData(
            sampleContacts,
            filter = ExportFilter.VERIFIED_ONLY,
            format = ExportFormat.CSV,
            packaging = ExportPackaging.SEPARATE_FILES
        )

        assertTrue(separateCsv.contains("=== VALID CONTACTS ==="))
        assertTrue(separateCsv.contains("=== INVALID CONTACTS ==="))
        assertFalse(separateCsv.contains("=== PENDING CONTACTS ==="))
    }

    @Test
    fun testExportDataByCategories() {
        val categoriesMap = ContactExporter.exportDataByCategories(
            sampleContacts,
            filter = ExportFilter.ALL,
            format = ExportFormat.CSV
        )

        assertEquals(3, categoriesMap.size)
        assertTrue(categoriesMap.containsKey(ContactStatus.VALID))
        assertTrue(categoriesMap.containsKey(ContactStatus.INVALID))
        assertTrue(categoriesMap.containsKey(ContactStatus.PENDING))

        val validCsv = categoriesMap[ContactStatus.VALID]!!
        assertTrue(validCsv.contains("+18005550199"))
        assertTrue(validCsv.contains("+442079460912"))
        assertFalse(validCsv.contains("5550198"))

        val invalidCsv = categoriesMap[ContactStatus.INVALID]!!
        assertTrue(invalidCsv.contains("5550198"))
        assertFalse(invalidCsv.contains("+18005550199"))
    }

    @Test
    fun testGetFileNameForStatus() {
        assertEquals("valid_contacts.csv", ContactExporter.getFileNameForStatus(ContactStatus.VALID, ExportFormat.CSV))
        assertEquals("invalid_contacts.txt", ContactExporter.getFileNameForStatus(ContactStatus.INVALID, ExportFormat.PLAIN_TEXT))
        assertEquals("pending_contacts.csv", ContactExporter.getFileNameForStatus(ContactStatus.PENDING, ExportFormat.CSV))
    }

    @Test
    fun testGetCategoryHeader() {
        assertEquals("=== VALID CONTACTS ===", ContactExporter.getCategoryHeader(ContactStatus.VALID))
        assertEquals("=== INVALID CONTACTS ===", ContactExporter.getCategoryHeader(ContactStatus.INVALID))
        assertEquals("=== PENDING CONTACTS ===", ContactExporter.getCategoryHeader(ContactStatus.PENDING))
    }
}
