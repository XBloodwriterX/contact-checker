package com.bloodwriter.contactchecker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bloodwriter.contactchecker.data.model.VerificationState
import com.bloodwriter.contactchecker.ui.theme.ContactCheckerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCheckerTopBar(
    verificationState: VerificationState,
    onLoadSampleData: () -> Unit,
    onClearQueue: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PhoneInTalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Contact Checker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                VerificationStateBadge(verificationState = verificationState)
            }
        },
        actions = {
            IconButton(onClick = onOpenExport) {
                Icon(
                    imageVector = Icons.Rounded.FileUpload,
                    contentDescription = "Export Contacts",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onLoadSampleData) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = "Load Sample Data",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClearQueue) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear Queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
fun VerificationStateBadge(
    verificationState: VerificationState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, icon, label) = when (verificationState) {
        VerificationState.IDLE -> StateBadgeSpec(
            bgColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            fgColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Rounded.PlayArrow,
            label = "IDLE"
        )
        VerificationState.RUNNING -> StateBadgeSpec(
            bgColor = Color(0xFF1B5E20),
            fgColor = Color.White,
            icon = Icons.Rounded.PlayArrow,
            label = "RUNNING"
        )
        VerificationState.PAUSED -> StateBadgeSpec(
            bgColor = Color(0xFFE65100),
            fgColor = Color.White,
            icon = Icons.Rounded.Pause,
            label = "PAUSED"
        )
        VerificationState.STOPPED -> StateBadgeSpec(
            bgColor = MaterialTheme.colorScheme.error,
            fgColor = MaterialTheme.colorScheme.onError,
            icon = Icons.Rounded.Stop,
            label = "STOPPED"
        )
        VerificationState.COMPLETED -> StateBadgeSpec(
            bgColor = MaterialTheme.colorScheme.primary,
            fgColor = MaterialTheme.colorScheme.onPrimary,
            icon = Icons.Rounded.CheckCircle,
            label = "DONE"
        )
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class StateBadgeSpec(
    val bgColor: Color,
    val fgColor: Color,
    val icon: ImageVector,
    val label: String
)

@Preview
@Composable
fun ContactCheckerTopBarPreview() {
    ContactCheckerTheme {
        ContactCheckerTopBar(
            verificationState = VerificationState.RUNNING,
            onLoadSampleData = {},
            onClearQueue = {},
            onOpenExport = {}
        )
    }
}
