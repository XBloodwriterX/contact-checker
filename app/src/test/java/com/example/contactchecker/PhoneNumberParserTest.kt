package com.example.contactchecker

import com.example.contactchecker.data.model.ContactStatus
import com.example.contactchecker.utils.PhoneNumberParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberParserTest {

    @Test
    fun testCleanPhoneNumber_preservesPlusAndDigits() {
        val input1 = "+1 (800) 555-0199"
        val input2 = "555-0123"
        val input3 = "+ 44 20 7946 0912"

        assertEquals("+18005550199", PhoneNumberParser.cleanPhoneNumber(input1))
        assertEquals("5550123", PhoneNumberParser.cleanPhoneNumber(input2))
        assertEquals("+442079460912", PhoneNumberParser.cleanPhoneNumber(input3))
    }

    @Test
    fun testIsValidPhoneNumber() {
        assertTrue(PhoneNumberParser.isValidPhoneNumber("+18005550199"))
        assertTrue(PhoneNumberParser.isValidPhoneNumber("5550123"))
        assertFalse(PhoneNumberParser.isValidPhoneNumber("12"))
        assertFalse(PhoneNumberParser.isValidPhoneNumber(""))
    }

    @Test
    fun testParseInputText_multipleSeparators() {
        val input = "+1 (800) 555-0199, 555-0123; +442079460912\n1234567"
        val items = PhoneNumberParser.parseInputText(input)

        assertEquals(4, items.size)
        assertEquals("+18005550199", items[0].phoneNumber)
        assertEquals("5550123", items[1].phoneNumber)
        assertEquals("+442079460912", items[2].phoneNumber)
        assertEquals("1234567", items[3].phoneNumber)

        assertTrue(items.all { it.status == ContactStatus.PENDING })
    }

    @Test
    fun testParseInputText_invalidNumberMarkedInvalid() {
        val input = "+18005550199, ab, 555-0123"
        val items = PhoneNumberParser.parseInputText(input)

        assertEquals(3, items.size)
        assertEquals(ContactStatus.PENDING, items[0].status)
        assertEquals(ContactStatus.INVALID, items[1].status)
        assertEquals(ContactStatus.PENDING, items[2].status)
    }
}
