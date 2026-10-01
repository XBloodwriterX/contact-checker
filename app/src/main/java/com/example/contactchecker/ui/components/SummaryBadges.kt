package com.example.contactchecker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.contactchecker.ui.theme.ContactCheckerTheme

@Composable
fun SummaryBadges(
    totalCount: Int,
    validCount: Int,
    invalidCount: Int,
    pendingCount: Int,
    inProgressCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Total
        SummaryChip(
            label = "Total",
            count = totalCount,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Rounded.FormatListNumbered
        )

        // Valid (Green)
        SummaryChip(
            label = "Valid",
            count = validCount,
            containerColor = Color(0xFFE8F5E9),
            contentColor = Color(0xFF1B5E20),
            icon = Icons.Rounded.CheckCircle
        )

        // Invalid (Red)
        SummaryChip(
            label = "Invalid",
            count = invalidCount,
            containerColor = Color(0xFFFFEBEE),
            contentColor = Color(0xFFC62828),
            icon = Icons.Rounded.Error
        )

        // Pending (Gray)
        SummaryChip(
            label = "Pending",
            count = pendingCount,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Rounded.HourglassEmpty
        )

        // In Progress (Blue, if active)
        if (inProgressCount > 0) {
            SummaryChip(
                label = "Active",
                count = inProgressCount,
                containerColor = Color(0xFFE3F2FD),
                contentColor = Color(0xFF1565C0),
                icon = Icons.Rounded.Call
            )
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SummaryBadgesPreview() {
    ContactCheckerTheme {
        SummaryBadges(
            totalCount = 10,
            validCount = 5,
            invalidCount = 2,
            pendingCount = 3,
            inProgressCount = 1,
            modifier = Modifier.padding(16.dp)
        )
    }
}
