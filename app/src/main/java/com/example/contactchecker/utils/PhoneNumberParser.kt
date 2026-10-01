package com.example.contactchecker.utils

import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus

object PhoneNumberParser {

    /**
     * Clean phone number by preserving '+' at start (if present) and digits.
     */
    fun cleanPhoneNumber(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""

        val hasLeadingPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }

        return if (hasLeadingPlus && digits.isNotEmpty()) {
            "+$digits"
        } else {
            digits
        }
    }

    /**
     * Validates if cleaned phone number is valid (e.g. contains 3 to 15 digits).
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digits = phoneNumber.filter { it.isDigit() }
        return digits.length in 3..15
    }

    /**
     * Parses input text separated by commas, newlines, semicolons, or spaces into ContactItem list.
     */
    fun parseInputText(inputText: String): List<ContactItem> {
        if (inputText.isBlank()) return emptyList()

        // Split by common delimiters: comma, newline, semicolon, tab
        val rawTokens = inputText.split(Regex("[\\n\\r;,\\t]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val items = mutableListOf<ContactItem>()

        for (token in rawTokens) {
            val spaceTokens = token.split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (spaceTokens.size > 1 && spaceTokens.all { isValidPhoneNumber(cleanPhoneNumber(it)) }) {
                for (subToken in spaceTokens) {
                    items.add(createContactItem(subToken))
                }
            } else {
                items.add(createContactItem(token))
            }
        }

        return items
    }

    private fun createContactItem(rawToken: String): ContactItem {
        val cleaned = cleanPhoneNumber(rawToken)
        val isValid = isValidPhoneNumber(cleaned)
        return ContactItem(
            rawInput = rawToken,
            phoneNumber = cleaned,
            status = if (isValid) ContactStatus.PENDING else ContactStatus.INVALID,
            note = if (isValid) null else "Invalid phone number format"
        )
    }
}
