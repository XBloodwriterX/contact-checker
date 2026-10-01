package com.example.contactchecker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.contactchecker.data.model.ContactItem
import com.example.contactchecker.data.model.ContactStatus
import com.example.contactchecker.ui.theme.ContactCheckerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactQueueList(
    contacts: List<ContactItem>,
    currentIndex: Int,
    onDeleteContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (contacts.isEmpty()) {
        EmptyQueueView(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = contacts,
                key = { _, item -> item.id }
            ) { index, contact ->
                ContactItemCard(
                    contact = contact,
                    index = index,
                    isCurrentlyProcessing = index == currentIndex && contact.status == ContactStatus.IN_PROGRESS,
                    onDelete = { onDeleteContact(contact.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: ContactItem,
    index: Int,
    isCurrentlyProcessing: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusSpec = getStatusSpec(contact.status)
    val animatedBgColor by animateColorAsState(
        targetValue = if (isCurrentlyProcessing) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "BgColorAnimation"
    )

    val dateFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val formattedTime = remember(contact.timestamp) { dateFormat.format(Date(contact.timestamp)) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyProcessing) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status Indicator Icon / Spinner
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (contact.status == ContactStatus.IN_PROGRESS) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Surface(
                        color = statusSpec.containerColor,
                        contentColor = statusSpec.contentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusSpec.icon,
                                contentDescription = statusSpec.label,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Phone Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = contact.phoneNumber.ifBlank { contact.rawInput },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusTag(spec = statusSpec)
                }

                if (contact.rawInput.isNotBlank() && contact.rawInput != contact.phoneNumber) {
                    Text(
                        text = "Raw input: ${contact.rawInput}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!contact.note.isNullOrBlank()) {
                    Text(
                        text = contact.note,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusSpec.contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Added at $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remove Contact",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusTag(
    spec: StatusSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        color = spec.containerColor,
        contentColor = spec.contentColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyQueueView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FormatListNumbered,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Contact Queue is Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Paste phone numbers in the input box above and tap 'Parse & Add to Queue' to start verification.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}

private data class StatusSpec(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

private fun getStatusSpec(status: ContactStatus): StatusSpec {
    return when (status) {
        ContactStatus.PENDING -> StatusSpec(
            label = "PENDING",
            icon = Icons.Rounded.HourglassEmpty,
            containerColor = Color(0xFFF5F5F5),
            contentColor = Color(0xFF616161)
        )
        ContactStatus.IN_PROGRESS -> StatusSpec(
            label = "DIALING",
            icon = Icons.Rounded.Call,
            containerColor = Color(0xFFE3F2FD),
            contentColor = Color(0xFF1565C0)
        )
        ContactStatus.VALID -> StatusSpec(
            label = "VALID",
            icon = Icons.Rounded.CheckCircle,
            containerColor = Color(0xFFE8F5E9),
            contentColor = Color(0xFF2E7D32)
        )
        ContactStatus.INVALID -> StatusSpec(
            label = "INVALID",
            icon = Icons.Rounded.Error,
            containerColor = Color(0xFFFFEBEE),
            contentColor = Color(0xFFC62828)
        )
        ContactStatus.SKIPPED -> StatusSpec(
            label = "SKIPPED",
            icon = Icons.Rounded.SkipNext,
            containerColor = Color(0xFFFFF3E0),
            contentColor = Color(0xFFE65100)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContactQueueListPreview() {
    val sampleContacts = listOf(
        ContactItem(id = "1", rawInput = "+18005550199", phoneNumber = "+18005550199", status = ContactStatus.VALID, note = "Ringing tone detected"),
        ContactItem(id = "2", rawInput = "555-0198", phoneNumber = "5550198", status = ContactStatus.IN_PROGRESS, note = "Dialing..."),
        ContactItem(id = "3", rawInput = "invalid_num", phoneNumber = "invalid_num", status = ContactStatus.INVALID, note = "Invalid phone number format"),
        ContactItem(id = "4", rawInput = "+442079460912", phoneNumber = "+442079460912", status = ContactStatus.PENDING)
    )

    ContactCheckerTheme {
        ContactQueueList(
            contacts = sampleContacts,
            currentIndex = 1,
            onDeleteContact = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
