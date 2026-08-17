package com.example.smarthomeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarthomeapp.ui.theme.IconSize
import com.example.smarthomeapp.ui.theme.Spacing

/**
 * A safety alert or an action failure.
 *
 * Given an icon and a leading colour bar so it separates from the device cards below it — these
 * are the one thing on the dashboard the user is expected to act on, and previously they were a
 * flat block of text the same shape as everything else. The action sits on its own line rather
 * than beside the message, so a long alert (the worker writes full sentences) never squeezes the
 * button down to two characters.
 */
@Composable
fun AlertBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Dismiss",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                start = Spacing.md,
                end = Spacing.sm,
                top = Spacing.md,
                bottom = Spacing.sm,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(IconSize.sm),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 6.dp),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = Spacing.xs),
                ) {
                    Text(
                        text = dismissLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}
