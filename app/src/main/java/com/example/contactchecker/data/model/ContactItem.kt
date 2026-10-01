package com.example.contactchecker.data.model

import java.util.UUID

data class ContactItem(
    val id: String = UUID.randomUUID().toString(),
    val rawInput: String,
    val phoneNumber: String,
    val status: ContactStatus = ContactStatus.PENDING,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
