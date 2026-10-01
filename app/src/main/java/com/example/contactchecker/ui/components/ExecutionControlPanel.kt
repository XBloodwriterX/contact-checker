package com.example.contactchecker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.contactchecker.data.model.VerificationState
import com.example.contactchecker.ui.theme.ContactCheckerTheme

@Composable
fun ExecutionControlPanel(
    verificationState: VerificationState,
    progress: Float,
    currentIndex: Int,
    totalCount: Int,
    currentPhoneNumber: String?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "ProgressAnimation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Execution Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Button
                val isStartEnabled = totalCount > 0 && (
                    verificationState == VerificationState.IDLE ||
                        verificationState == VerificationState.STOPPED ||
                        verificationState == VerificationState.COMPLETED
                    )
                Button(
                    onClick = onStart,
                    enabled = isStartEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start")
                }

                // Pause / Resume Button
                val isPauseResumeEnabled = verificationState == VerificationState.RUNNING ||
                    verificationState == VerificationState.PAUSED
                val isPaused = verificationState == VerificationState.PAUSED

                FilledTonalButton(
                    onClick = { if (isPaused) onResume() else onPause() },
                    enabled = isPauseResumeEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFE65100),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPaused) "Resume" else "Pause")
                }

                // Stop Button
                val isStopEnabled = verificationState == VerificationState.RUNNING ||
                    verificationState == VerificationState.PAUSED
                Button(
                    onClick = onStop,
                    enabled = isStopEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }

                // Reset Button
                val isResetEnabled = totalCount > 0 && (
                    verificationState == VerificationState.IDLE ||
                        verificationState == VerificationState.PAUSED ||
                        verificationState == VerificationState.STOPPED ||
                        verificationState == VerificationState.COMPLETED
                    )
                OutlinedButton(
                    onClick = onReset,
                    enabled = isResetEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verification Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                val percentInt = (animatedProgress * 100).toInt()
                Text(
                    text = "$percentInt% (${currentIndex.coerceAtMost(totalCount)} / $totalCount)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (verificationState == VerificationState.RUNNING || verificationState == VerificationState.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentPhoneNumber != null) {
                        "Currently verifying: $currentPhoneNumber (${currentIndex + 1} of $totalCount)"
                    } else {
                        "Processing item ${currentIndex + 1} of $totalCount..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExecutionControlPanelPreview() {
    ContactCheckerTheme {
        ExecutionControlPanel(
            verificationState = VerificationState.RUNNING,
            progress = 0.45f,
            currentIndex = 2,
            totalCount = 6,
            currentPhoneNumber = "+18005550199",
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onReset = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
