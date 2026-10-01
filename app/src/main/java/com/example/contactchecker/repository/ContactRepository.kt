package com.example.contactchecker.repository

import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.VerificationState
import kotlinx.coroutines.flow.StateFlow

interface ContactRepository {
    val contacts: StateFlow<List<ContactItem>>
    val verificationState: StateFlow<VerificationState>
    val currentIndex: StateFlow<Int>
    val progress: StateFlow<Float>

    fun setContacts(newContacts: List<ContactItem>)
    fun addContactsFromText(inputText: String)
    fun clearContacts()
    fun removeContact(id: String)

    fun startVerification(timeoutMs: Long = 8000L)
    fun pauseVerification()
    fun resumeVerification()
    fun stopVerification()
    fun resetVerification()
}
