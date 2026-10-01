package com.example.contactchecker

import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import com.example.contactchecker.utils.ContactExporter
import com.example.contactchecker.utils.ExportFilter
import com.example.contactchecker.utils.ExportFormat
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
        )
    )

    @Test
    fun testExportToPlainText_AllContacts() {
        val plainText = ContactExporter.exportToPlainText(sampleContacts, validOnly = false)
        val lines = plainText.split("\n")

        assertEquals(3, lines.size)
        assertEquals("+18005550199", lines[0])
        assertEquals("5550198", lines[1])
        assertEquals("+442079460912", lines[2])
    }

    @Test
    fun testExportToPlainText_ValidOnly() {
        val plainText = ContactExporter.exportToPlainText(sampleContacts, validOnly = true)
        val lines = plainText.split("\n")

        assertEquals(2, lines.size)
        assertEquals("+18005550199", lines[0])
        assertEquals("+442079460912", lines[1])
        assertFalse(plainText.contains("5550198"))
    }

    @Test
    fun testExportToCsv_AllContacts() {
        val csv = ContactExporter.exportToCsv(sampleContacts, validOnly = false)
        val lines = csv.split("\n")

        assertEquals(4, lines.size) // Header + 3 rows
        assertEquals("Phone Number,Status,Verification Note,Timestamp", lines[0])

        assertTrue(lines[1].startsWith("+18005550199,VALID,Ringing tone detected"))
        assertTrue(lines[2].contains("INVALID"))
        assertTrue(lines[2].contains("5550198"))
        // Second row note has comma so it gets quoted: "Number disconnected, no ring"
        assertTrue(lines[2].contains("\"Number disconnected, no ring\""))
        assertTrue(lines[3].startsWith("+442079460912,VALID,Ringing tone detected"))
    }

    @Test
    fun testExportToCsv_ValidOnly() {
        val csv = ContactExporter.exportToCsv(sampleContacts, validOnly = true)
        val lines = csv.split("\n")

        assertEquals(3, lines.size) // Header + 2 rows
        assertEquals("Phone Number,Status,Verification Note,Timestamp", lines[0])
        assertFalse(csv.contains("5550198"))
        assertFalse(csv.contains("INVALID"))
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
        // Escaped note should wrap with quotes and double the internal quotes
        assertTrue(lines[1].contains("\"Note with \"\"quotes\"\" and , comma\""))
    }

    @Test
    fun testExportData_EnumDispatch() {
        val csvResult = ContactExporter.exportData(
            sampleContacts,
            filter = ExportFilter.VALID_ONLY,
            format = ExportFormat.CSV
        )
        assertTrue(csvResult.startsWith("Phone Number,Status,Verification Note,Timestamp"))
        assertFalse(csvResult.contains("5550198"))

        val textResult = ContactExporter.exportData(
            sampleContacts,
            filter = ExportFilter.ALL,
            format = ExportFormat.PLAIN_TEXT
        )
        val lines = textResult.split("\n")
        assertEquals(3, lines.size)
    }
}
