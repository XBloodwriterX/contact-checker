package com.bloodwriter.contactchecker.ui.components

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
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import com.bloodwriter.contactchecker.data.model.VerificationState
import com.bloodwriter.contactchecker.ui.theme.ContactCheckerTheme

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
    modifier: Modifier = Modifier,
    onClearQueue: (() -> Unit)? = null,
    onLoadSample: (() -> Unit)? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "ProgressAnimation"
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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

            // Row 1: Primary Verification Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start / Pause / Resume Button
                when (verificationState) {
                    VerificationState.RUNNING -> {
                        FilledTonalButton(
                            onClick = onPause,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFE65100),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause")
                        }
                    }
                    VerificationState.PAUSED -> {
                        Button(
                            onClick = onResume,
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume")
                        }
                    }
                    else -> {
                        val isStartEnabled = totalCount > 0
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start")
                        }
                    }
                }

                // Stop Button
                val isStopEnabled = (verificationState == VerificationState.RUNNING) ||
                    (verificationState == VerificationState.PAUSED)
                FilledTonalButton(
                    onClick = onStop,
                    enabled = isStopEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Management Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }

                // Clear Queue or Sample Data Action Button
                val isActionEnabled = verificationState != VerificationState.RUNNING &&
                    verificationState != VerificationState.PAUSED

                if (totalCount == 0 && onLoadSample != null) {
                    OutlinedButton(
                        onClick = onLoadSample,
                        enabled = isActionEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sample Data")
                    }
                } else {
                    val isClearEnabled = totalCount > 0
                    OutlinedButton(
                        onClick = { onClearQueue?.invoke() },
                        enabled = isClearEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Queue")
                    }
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

@Preview(name = "Execution Controls - Idle", showBackground = true)
@Composable
fun ExecutionControlPanelIdlePreview() {
    ContactCheckerTheme {
        ExecutionControlPanel(
            verificationState = VerificationState.IDLE,
            progress = 0f,
            currentIndex = 0,
            totalCount = 5,
            currentPhoneNumber = null,
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onReset = {},
            onClearQueue = {},
            onLoadSample = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Execution Controls - Running", showBackground = true)
@Composable
fun ExecutionControlPanelRunningPreview() {
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
            onClearQueue = {},
            onLoadSample = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Execution Controls - Paused", showBackground = true)
@Composable
fun ExecutionControlPanelPausedPreview() {
    ContactCheckerTheme {
        ExecutionControlPanel(
            verificationState = VerificationState.PAUSED,
            progress = 0.45f,
            currentIndex = 2,
            totalCount = 6,
            currentPhoneNumber = "+18005550199",
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onReset = {},
            onClearQueue = {},
            onLoadSample = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Execution Controls - Empty Queue", showBackground = true)
@Composable
fun ExecutionControlPanelEmptyPreview() {
    ContactCheckerTheme {
        ExecutionControlPanel(
            verificationState = VerificationState.IDLE,
            progress = 0f,
            currentIndex = 0,
            totalCount = 0,
            currentPhoneNumber = null,
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onReset = {},
            onClearQueue = {},
            onLoadSample = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
