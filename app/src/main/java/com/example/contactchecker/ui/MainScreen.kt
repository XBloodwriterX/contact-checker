package com.example.contactchecker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import com.example.contactchecker.data.model.VerificationState
import com.example.contactchecker.ui.components.ContactCheckerTopBar
import com.example.contactchecker.ui.components.ContactQueueList
import com.example.contactchecker.ui.components.ExecutionControlPanel
import com.example.contactchecker.ui.components.ExportDialog
import com.example.contactchecker.ui.components.InputSection
import com.example.contactchecker.ui.components.PermissionBanner
import com.example.contactchecker.ui.components.PermissionHandler
import com.example.contactchecker.ui.components.SummaryBadges
import com.example.contactchecker.ui.theme.ContactCheckerTheme
import com.example.contactchecker.utils.ContactExporter
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PermissionHandler(
        onPermissionResult = { granted ->
            try {
                viewModel.updatePermissionGranted(granted)
            } catch (_: Throwable) {}
        }
    ) { permissionsState ->
        val showPermissionBanner = try {
            !permissionsState.allPermissionsGranted
        } catch (_: Throwable) {
            false
        }

        MainScreenContent(
            uiState = uiState,
            showPermissionBanner = showPermissionBanner,
            onRequestPermissions = {
                try {
                    permissionsState.launchMultiplePermissionRequest()
                } catch (_: Throwable) {}
            },
            onInputTextChanged = viewModel::onRawInputChanged,
            onParseAndAdd = viewModel::parseAndAddInput,
            onClearInput = viewModel::clearRawInput,
            onLoadSample = viewModel::loadSampleData,
            onStart = viewModel::startVerification,
            onPause = viewModel::pauseVerification,
            onResume = viewModel::resumeVerification,
            onStop = viewModel::stopVerification,
            onReset = viewModel::resetVerification,
            onClearQueue = viewModel::clearContacts,
            onDeleteContact = viewModel::removeContact,
            modifier = modifier
        )
    }
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    showPermissionBanner: Boolean,
    onRequestPermissions: () -> Unit,
    onInputTextChanged: (String) -> Unit,
    onParseAndAdd: () -> Unit,
    onClearInput: () -> Unit,
    onLoadSample: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onClearQueue: () -> Unit,
    onDeleteContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportDialog(
            contacts = uiState.contactList,
            onDismiss = { showExportDialog = false },
            onCopy = { content, _ ->
                ContactExporter.copyToClipboard(context, content)
                showExportDialog = false
            },
            onShare = { content, format ->
                ContactExporter.shareContent(context, content, format)
                showExportDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            ContactCheckerTopBar(
                verificationState = uiState.verificationState,
                onLoadSampleData = onLoadSample,
                onClearQueue = onClearQueue,
                onOpenExport = { showExportDialog = true }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            val currentContact = uiState.contactList.getOrNull(uiState.currentIndex)
            val currentPhoneNumber = currentContact?.phoneNumber

            if (isWideScreen) {
                // Adaptive Layout: Two-Pane Split View for Tablet / Wide Screens
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Pane: Controls & Input
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (showPermissionBanner) {
                            PermissionBanner(
                                onRequestPermissions = onRequestPermissions
                            )
                        }

                        InputSection(
                            inputText = uiState.rawInputText,
                            onInputTextChanged = onInputTextChanged,
                            onParseAndAdd = onParseAndAdd,
                            onClearInput = onClearInput,
                            onLoadSample = onLoadSample
                        )

                        ExecutionControlPanel(
                            verificationState = uiState.verificationState,
                            progress = uiState.progress,
                            currentIndex = uiState.currentIndex,
                            totalCount = uiState.contactList.size,
                            currentPhoneNumber = currentPhoneNumber,
                            onStart = onStart,
                            onPause = onPause,
                            onResume = onResume,
                            onStop = onStop,
                            onReset = onReset
                        )

                        SummaryBadges(
                            totalCount = uiState.contactList.size,
                            validCount = uiState.validCount,
                            invalidCount = uiState.invalidCount,
                            pendingCount = uiState.pendingCount,
                            inProgressCount = uiState.inProgressCount
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Right Pane: Contact Queue List
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Verification Queue (${uiState.contactList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        ContactQueueList(
                            contacts = uiState.contactList,
                            currentIndex = uiState.currentIndex,
                            onDeleteContact = onDeleteContact,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // Single Pane Layout for Phone / Compact Screens
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showPermissionBanner) {
                        PermissionBanner(
                            onRequestPermissions = onRequestPermissions
                        )
                    }

                    InputSection(
                        inputText = uiState.rawInputText,
                        onInputTextChanged = onInputTextChanged,
                        onParseAndAdd = onParseAndAdd,
                        onClearInput = onClearInput,
                        onLoadSample = onLoadSample
                    )

                    ExecutionControlPanel(
                        verificationState = uiState.verificationState,
                        progress = uiState.progress,
                        currentIndex = uiState.currentIndex,
                        totalCount = uiState.contactList.size,
                        currentPhoneNumber = currentPhoneNumber,
                        onStart = onStart,
                        onPause = onPause,
                        onResume = onResume,
                        onStop = onStop,
                        onReset = onReset
                    )

                    SummaryBadges(
                        totalCount = uiState.contactList.size,
                        validCount = uiState.validCount,
                        invalidCount = uiState.invalidCount,
                        pendingCount = uiState.pendingCount,
                        inProgressCount = uiState.inProgressCount
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Contact Status Queue (${uiState.contactList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ContactQueueList(
                        contacts = uiState.contactList,
                        currentIndex = uiState.currentIndex,
                        onDeleteContact = onDeleteContact,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Previews
@Preview(name = "Phone Light Theme", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun MainScreenPhoneLightPreview() {
    val sampleContacts = listOf(
        ContactItem(id = "1", rawInput = "+18005550199", phoneNumber = "+18005550199", status = ContactStatus.VALID, note = "Ringing tone detected"),
        ContactItem(id = "2", rawInput = "555-0198", phoneNumber = "5550198", status = ContactStatus.IN_PROGRESS, note = "Dialing..."),
        ContactItem(id = "3", rawInput = "invalid_num", phoneNumber = "invalid_num", status = ContactStatus.INVALID, note = "Invalid phone number format"),
        ContactItem(id = "4", rawInput = "+442079460912", phoneNumber = "+442079460912", status = ContactStatus.PENDING)
    )

    val sampleUiState = MainUiState(
        rawInputText = "+18005550199\n555-0198",
        contactList = sampleContacts,
        verificationState = VerificationState.RUNNING,
        progress = 0.5f,
        currentIndex = 1,
        validCount = 1,
        invalidCount = 1,
        pendingCount = 1,
        inProgressCount = 1,
        permissionGranted = true
    )

    ContactCheckerTheme(darkTheme = false) {
        MainScreenContent(
            uiState = sampleUiState,
            showPermissionBanner = false,
            onRequestPermissions = {},
            onInputTextChanged = {},
            onParseAndAdd = {},
            onClearInput = {},
            onLoadSample = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onReset = {},
            onClearQueue = {},
            onDeleteContact = {}
        )
    }
}

@Preview(name = "Phone Dark Theme", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun MainScreenPhoneDarkPreview() {
    val sampleContacts = listOf(
        ContactItem(id = "1", rawInput = "+18005550199", phoneNumber = "+18005550199", status = ContactStatus.VALID, note = "Ringing tone detected"),
        ContactItem(id = "2", rawInput = "555-0198", phoneNumber = "5550198", status = ContactStatus.PENDING)
    )

    val sampleUiState = MainUiState(
        rawInputText = "",
        contactList = sampleContacts,
        verificationState = VerificationState.IDLE,
        progress = 0f,
        currentIndex = 0,
        validCount = 1,
        invalidCount = 0,
        pendingCount = 1,
        inProgressCount = 0,
        permissionGranted = false
    )

    ContactCheckerTheme(darkTheme = true) {
        MainScreenContent(
            uiState = sampleUiState,
            showPermissionBanner = true,
            onRequestPermissions = {},
            onInputTextChanged = {},
            onParseAndAdd = {},
            onClearInput = {},
            onLoadSample = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onReset = {},
            onClearQueue = {},
            onDeleteContact = {}
        )
    }
}

@Preview(name = "Tablet Wide Screen", showBackground = true, widthDp = 1024, heightDp = 768)
@Composable
fun MainScreenTabletPreview() {
    val sampleContacts = listOf(
        ContactItem(id = "1", rawInput = "+18005550199", phoneNumber = "+18005550199", status = ContactStatus.VALID, note = "Ringing tone detected"),
        ContactItem(id = "2", rawInput = "555-0198", phoneNumber = "5550198", status = ContactStatus.IN_PROGRESS, note = "Dialing..."),
        ContactItem(id = "3", rawInput = "invalid_num", phoneNumber = "invalid_num", status = ContactStatus.INVALID, note = "Invalid phone number format"),
        ContactItem(id = "4", rawInput = "+442079460912", phoneNumber = "+442079460912", status = ContactStatus.PENDING)
    )

    val sampleUiState = MainUiState(
        rawInputText = "+18005550199\n555-0198",
        contactList = sampleContacts,
        verificationState = VerificationState.RUNNING,
        progress = 0.5f,
        currentIndex = 1,
        validCount = 1,
        invalidCount = 1,
        pendingCount = 1,
        inProgressCount = 1,
        permissionGranted = true
    )

    ContactCheckerTheme(darkTheme = false) {
        MainScreenContent(
            uiState = sampleUiState,
            showPermissionBanner = false,
            onRequestPermissions = {},
            onInputTextChanged = {},
            onParseAndAdd = {},
            onClearInput = {},
            onLoadSample = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onReset = {},
            onClearQueue = {},
            onDeleteContact = {}
        )
    }
}
