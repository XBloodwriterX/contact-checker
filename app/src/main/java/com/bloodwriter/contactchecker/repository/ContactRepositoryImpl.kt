package com.bloodwriter.contactchecker.repository

import android.content.Context
import com.bloodwriter.contactchecker.data.model.ContactItem
import com.bloodwriter.contactchecker.data.model.ContactStatus
import com.bloodwriter.contactchecker.data.model.VerificationState
import com.bloodwriter.contactchecker.telephony.CallState
import com.bloodwriter.contactchecker.telephony.ContactCheckerCallManager
import com.bloodwriter.contactchecker.utils.PhoneNumberParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactRepositoryImpl(
    private val context: Context,
    private val callManager: ContactCheckerCallManager = ContactCheckerCallManager,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : ContactRepository {

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    override val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _verificationState = MutableStateFlow(VerificationState.IDLE)
    override val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    override val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    override val progress: StateFlow<Float> = combine(_contacts, _currentIndex, _verificationState) { list, index, state ->
        if (list.isEmpty()) {
            0f
        } else if (state == VerificationState.COMPLETED) {
            1f
        } else {
            (index.toFloat() / list.size.toFloat()).coerceIn(0f, 1f)
        }
    }.stateIn(externalScope, SharingStarted.Eagerly, 0f)

    private var verificationJob: Job? = null

    override fun setContacts(newContacts: List<ContactItem>) {
        stopVerification()
        _contacts.value = newContacts
        _currentIndex.value = 0
        _verificationState.value = VerificationState.IDLE
    }

    override fun addContactsFromText(inputText: String) {
        val newItems = PhoneNumberParser.parseInputText(inputText)
        val updated = _contacts.value + newItems
        _contacts.value = updated
        if (_verificationState.value == VerificationState.COMPLETED) {
            _verificationState.value = VerificationState.IDLE
        }
    }

    override fun clearContacts() {
        stopVerification()
        _contacts.value = emptyList()
        _currentIndex.value = 0
        _verificationState.value = VerificationState.IDLE
    }

    override fun removeContact(id: String) {
        val updated = _contacts.value.filterNot { it.id == id }
        _contacts.value = updated
        if (_currentIndex.value >= updated.size && updated.isNotEmpty()) {
            _currentIndex.value = updated.size - 1
        }
    }

    override fun startVerification(timeoutMs: Long) {
        if (_verificationState.value == VerificationState.RUNNING) return
        if (_contacts.value.isEmpty()) return

        _verificationState.value = VerificationState.RUNNING
        try {
            callManager.registerTelephonyListener(context)
        } catch (_: Throwable) {
            // Ignore system listener registration error
        }

        verificationJob?.cancel()
        verificationJob = externalScope.launch {
            runVerificationLoop(timeoutMs)
        }
    }

    override fun pauseVerification() {
        if (_verificationState.value == VerificationState.RUNNING) {
            _verificationState.value = VerificationState.PAUSED
        }
    }

    override fun resumeVerification() {
        if (_verificationState.value == VerificationState.PAUSED) {
            _verificationState.value = VerificationState.RUNNING
        }
    }

    override fun stopVerification() {
        _verificationState.value = VerificationState.STOPPED
        verificationJob?.cancel()
        verificationJob = null
        try {
            callManager.disconnectCurrentCall(context)
        } catch (_: Throwable) {}
        try {
            callManager.unregisterTelephonyListener(context)
        } catch (_: Throwable) {}
    }

    override fun resetVerification() {
        stopVerification()
        _currentIndex.value = 0
        _contacts.value = _contacts.value.map {
            val isFormatValid = PhoneNumberParser.isValidPhoneNumber(it.phoneNumber)
            it.copy(
                status = if (isFormatValid) ContactStatus.PENDING else ContactStatus.INVALID,
                note = if (isFormatValid) null else "Invalid phone number format"
            )
        }
        _verificationState.value = VerificationState.IDLE
    }

    private suspend fun runVerificationLoop(timeoutMs: Long) {
        while (_currentIndex.value < _contacts.value.size && _verificationState.value != VerificationState.STOPPED) {
            while (_verificationState.value == VerificationState.PAUSED) {
                delay(300)
            }

            if (_verificationState.value != VerificationState.RUNNING) {
                break
            }

            val index = _currentIndex.value
            val currentList = _contacts.value
            if (index >= currentList.size) break

            val contact = currentList[index]

            if (contact.status == ContactStatus.SKIPPED) {
                _currentIndex.value = index + 1
                continue
            }

            if (!PhoneNumberParser.isValidPhoneNumber(contact.phoneNumber)) {
                updateContact(index) {
                    it.copy(
                        status = ContactStatus.INVALID,
                        note = "Invalid phone number format"
                    )
                }
                _currentIndex.value = index + 1
                continue
            }

            updateContact(index) {
                it.copy(
                    status = ContactStatus.IN_PROGRESS,
                    note = "Dialing number..."
                )
            }

            val callPlaced = try {
                callManager.placeCall(context, contact.phoneNumber)
            } catch (_: Throwable) {
                false
            }

            if (!callPlaced) {
                updateContact(index) {
                    it.copy(
                        status = ContactStatus.INVALID,
                        note = "Failed to place call"
                    )
                }
                _currentIndex.value = index + 1
                delay(500)
                continue
            }

            val monitorResult = monitorCallState(timeoutMs)

            when (monitorResult) {
                CallResult.RINGING_DETECTED -> {
                    updateContact(index) {
                        it.copy(
                            status = ContactStatus.VALID,
                            note = "Ringing / Answered tone detected"
                        )
                    }
                }
                CallResult.TIMED_OUT -> {
                    updateContact(index) {
                        it.copy(
                            status = ContactStatus.INVALID,
                            note = "Timed out ($timeoutMs ms) without ringing"
                        )
                    }
                }
                CallResult.DISCONNECTED_EARLY -> {
                    updateContact(index) {
                        it.copy(
                            status = ContactStatus.INVALID,
                            note = "Call disconnected before ringing tone"
                        )
                    }
                }
            }

            try {
                callManager.disconnectCurrentCall(context)
            } catch (_: Throwable) {}

            delay(1000)

            _currentIndex.value = index + 1
        }

        if (_verificationState.value == VerificationState.RUNNING && _currentIndex.value >= _contacts.value.size) {
            _verificationState.value = VerificationState.COMPLETED
        }

        try {
            callManager.unregisterTelephonyListener(context)
        } catch (_: Throwable) {}
    }

    private suspend fun monitorCallState(timeoutMs: Long): CallResult {
        val startTime = System.currentTimeMillis()
        var callHasBeenDialing = false

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (_verificationState.value == VerificationState.STOPPED) {
                return CallResult.TIMED_OUT
            }

            val callState = callManager.currentCallState.value
            when (callState) {
                CallState.DIALING, CallState.CONNECTING -> {
                    callHasBeenDialing = true
                }
                CallState.RINGING, CallState.ACTIVE -> {
                    return CallResult.RINGING_DETECTED
                }
                CallState.DISCONNECTED -> {
                    if (callHasBeenDialing) {
                        return CallResult.DISCONNECTED_EARLY
                    }
                }
                CallState.IDLE -> {
                    if (callHasBeenDialing) {
                        return CallResult.DISCONNECTED_EARLY
                    }
                }
            }
            delay(100)
        }

        return CallResult.TIMED_OUT
    }

    private fun updateContact(index: Int, updateBlock: (ContactItem) -> ContactItem) {
        val currentList = _contacts.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = updateBlock(currentList[index])
            _contacts.value = currentList
        }
    }

    private enum class CallResult {
        RINGING_DETECTED,
        TIMED_OUT,
        DISCONNECTED_EARLY
    }
}
