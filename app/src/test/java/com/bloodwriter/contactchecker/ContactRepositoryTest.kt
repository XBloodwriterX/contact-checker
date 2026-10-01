package com.bloodwriter.contactchecker

import android.content.Context
import android.content.ContextWrapper
import com.bloodwriter.contactchecker.data.model.ContactStatus
import com.bloodwriter.contactchecker.data.model.VerificationState
import com.bloodwriter.contactchecker.repository.ContactRepositoryImpl
import com.bloodwriter.contactchecker.telephony.CallState
import com.bloodwriter.contactchecker.telephony.ContactCheckerCallManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRepositoryTest {

    private val fakeContext: Context = object : ContextWrapper(null) {
        override fun getSystemService(name: String): Any? = null
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var repository: ContactRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ContactCheckerCallManager.isSimulationMode = true
        repository = ContactRepositoryImpl(
            context = fakeContext,
            callManager = ContactCheckerCallManager,
            externalScope = testScope
        )
    }

    @Test
    fun testAddContactsFromText() {
        val rawInput = "+18005550199, 555-0123"
        repository.addContactsFromText(rawInput)

        assertEquals(2, repository.contacts.value.size)
        assertEquals("+18005550199", repository.contacts.value[0].phoneNumber)
        assertEquals("5550123", repository.contacts.value[1].phoneNumber)
        assertEquals(0, repository.currentIndex.value)
        assertEquals(0f, repository.progress.value)
    }

    @Test
    fun testStartVerificationFlow_validRingingDetected() = runTest {
        repository.addContactsFromText("+18005550199")
        repository.startVerification(timeoutMs = 3000L)

        assertEquals(VerificationState.RUNNING, repository.verificationState.value)

        // Simulate call transitioning to RINGING
        ContactCheckerCallManager.onCallStateChanged(CallState.RINGING)

        advanceTimeBy(2000L)

        assertEquals(ContactStatus.VALID, repository.contacts.value[0].status)
    }

    @Test
    fun testPauseAndResumeVerification() {
        repository.addContactsFromText("+18005550199")
        repository.startVerification(timeoutMs = 3000L)

        repository.pauseVerification()
        assertEquals(VerificationState.PAUSED, repository.verificationState.value)

        repository.resumeVerification()
        assertEquals(VerificationState.RUNNING, repository.verificationState.value)
    }

    @Test
    fun testStopVerification() {
        repository.addContactsFromText("+18005550199")
        repository.startVerification(timeoutMs = 3000L)

        repository.stopVerification()
        assertEquals(VerificationState.STOPPED, repository.verificationState.value)
    }

    @Test
    fun testClearContacts_removesAllContactsAndResetsState() = runTest {
        repository.addContactsFromText("+18005550199, 555-0123")
        repository.startVerification(timeoutMs = 3000L)

        assertEquals(2, repository.contacts.value.size)
        assertEquals(VerificationState.RUNNING, repository.verificationState.value)

        repository.clearContacts()

        assertEquals(0, repository.contacts.value.size)
        assertEquals(0, repository.currentIndex.value)
        assertEquals(VerificationState.IDLE, repository.verificationState.value)
        assertEquals(0f, repository.progress.value)
    }
}
