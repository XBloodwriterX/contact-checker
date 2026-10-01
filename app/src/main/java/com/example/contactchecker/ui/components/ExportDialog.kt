package com.example.contactchecker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import com.example.contactchecker.ui.theme.ContactCheckerTheme
import com.example.contactchecker.utils.ContactExporter
import com.example.contactchecker.utils.ExportFilter
import com.example.contactchecker.utils.ExportFormat

@Composable
fun ExportDialog(
    contacts: List<ContactItem>,
    onDismiss: () -> Unit,
    onCopy: (content: String, format: ExportFormat) -> Unit,
    onShare: (content: String, format: ExportFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(ExportFilter.VALID_ONLY) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    val validCount = remember(contacts) {
        contacts.count { it.status == ContactStatus.VALID }
    }
    val totalCount = contacts.size

    val exportCount = if (selectedFilter == ExportFilter.VALID_ONLY) validCount else totalCount

    val exportedContent = remember(contacts, selectedFilter, selectedFormat) {
        ContactExporter.exportData(contacts, selectedFilter, selectedFormat)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.FileUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Export Contacts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Export Summary",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$exportCount ${if (exportCount == 1) "contact" else "contacts"} selected for export",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Filter Option
                Column {
                    Text(
                        text = "Contact Scope",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.selectableGroup()) {
                        FilterOptionRow(
                            title = "Valid Contacts Only ($validCount)",
                            selected = selectedFilter == ExportFilter.VALID_ONLY
                        ) {
                            selectedFilter = ExportFilter.VALID_ONLY
                        }
                        FilterOptionRow(
                            title = "All Contacts ($totalCount)",
                            selected = selectedFilter == ExportFilter.ALL
                        ) {
                            selectedFilter = ExportFilter.ALL
                        }
                    }
                }

                // Format Option
                Column {
                    Text(
                        text = "File / Output Format",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.selectableGroup()) {
                        FilterOptionRow(
                            title = "CSV Format (.csv)",
                            subtitle = "Phone Number, Status, Note, Timestamp",
                            selected = selectedFormat == ExportFormat.CSV
                        ) {
                            selectedFormat = ExportFormat.CSV
                        }
                        FilterOptionRow(
                            title = "Plain Text (.txt)",
                            subtitle = "One phone number per line",
                            selected = selectedFormat == ExportFormat.PLAIN_TEXT
                        ) {
                            selectedFormat = ExportFormat.PLAIN_TEXT
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        onCopy(exportedContent, selectedFormat)
                    },
                    enabled = exportCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Copy")
                }

                Button(
                    onClick = {
                        onShare(exportedContent, selectedFormat)
                    },
                    enabled = exportCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Share")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun FilterOptionRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExportDialogPreview() {
    val sampleContacts = listOf(
        ContactItem(id = "1", rawInput = "+18005550199", phoneNumber = "+18005550199", status = ContactStatus.VALID),
        ContactItem(id = "2", rawInput = "555-0198", phoneNumber = "5550198", status = ContactStatus.INVALID)
    )
    ContactCheckerTheme {
        Surface {
            ExportDialog(
                contacts = sampleContacts,
                onDismiss = {},
                onCopy = { _, _ -> },
                onShare = { _, _ -> }
            )
        }
    }
}
