package com.bloodwriter.contactchecker.repository

import com.bloodwriter.contactchecker.data.model.ContactItem
import com.bloodwriter.contactchecker.data.model.VerificationState
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
