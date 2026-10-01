package com.example.contactchecker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import com.example.contactchecker.data.model.VerificationState
import com.example.contactchecker.repository.ContactRepository
import com.example.contactchecker.repository.ContactRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MainUiState(
    val rawInputText: String = "",
    val contactList: List<ContactItem> = emptyList(),
    val verificationState: VerificationState = VerificationState.IDLE,
    val progress: Float = 0f,
    val currentIndex: Int = 0,
    val validCount: Int = 0,
    val invalidCount: Int = 0,
    val pendingCount: Int = 0,
    val inProgressCount: Int = 0,
    val permissionGranted: Boolean = false
)

class MainViewModel(
    application: Application,
    private val repository: ContactRepository = ContactRepositoryImpl(application)
) : AndroidViewModel(application) {

    private val _rawInputText = MutableStateFlow("")
    val rawInputText: StateFlow<String> = _rawInputText.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    val contactList: StateFlow<List<ContactItem>> = repository.contacts
    val verificationState: StateFlow<VerificationState> = repository.verificationState
    val currentIndex: StateFlow<Int> = repository.currentIndex
    val progress: StateFlow<Float> = repository.progress

    val validCount: StateFlow<Int> = contactList.map { list ->
        list.count { it.status == ContactStatus.VALID }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val invalidCount: StateFlow<Int> = contactList.map { list ->
        list.count { it.status == ContactStatus.INVALID }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val pendingCount: StateFlow<Int> = contactList.map { list ->
        list.count { it.status == ContactStatus.PENDING }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val inProgressCount: StateFlow<Int> = contactList.map { list ->
        list.count { it.status == ContactStatus.IN_PROGRESS }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val uiState: StateFlow<MainUiState> = combine(
        _rawInputText,
        contactList,
        verificationState,
        progress,
        currentIndex
    ) { rawInput, contacts, vState, prog, currIndex ->
        MainUiState(
            rawInputText = rawInput,
            contactList = contacts,
            verificationState = vState,
            progress = prog,
            currentIndex = currIndex,
            validCount = contacts.count { it.status == ContactStatus.VALID },
            invalidCount = contacts.count { it.status == ContactStatus.INVALID },
            pendingCount = contacts.count { it.status == ContactStatus.PENDING },
            inProgressCount = contacts.count { it.status == ContactStatus.IN_PROGRESS },
            permissionGranted = _permissionGranted.value
        )
    }.combine(_permissionGranted) { state, permGranted ->
        state.copy(permissionGranted = permGranted)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        MainUiState()
    )

    fun onRawInputChanged(newText: String) {
        _rawInputText.value = newText
    }

    fun parseAndAddInput() {
        val text = _rawInputText.value
        if (text.isNotBlank()) {
            repository.addContactsFromText(text)
            _rawInputText.value = ""
        }
    }

    fun clearRawInput() {
        _rawInputText.value = ""
    }

    fun loadSampleData() {
        val sampleText = """
            +18005550199
            +18005550100
            555-0198
            invalid_phone_num
            +442079460912
            +123
        """.trimIndent()
        _rawInputText.value = sampleText
    }

    fun startVerification() {
        repository.startVerification()
    }

    fun pauseVerification() {
        repository.pauseVerification()
    }

    fun resumeVerification() {
        repository.resumeVerification()
    }

    fun stopVerification() {
        repository.stopVerification()
    }

    fun resetVerification() {
        repository.resetVerification()
    }

    fun clearContacts() {
        repository.clearContacts()
    }

    fun removeContact(id: String) {
        repository.removeContact(id)
    }

    fun updatePermissionGranted(granted: Boolean) {
        _permissionGranted.value = granted
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: ContactRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (repository != null) {
            MainViewModel(application, repository) as T
        } else {
            MainViewModel(application) as T
        }
    }
}
