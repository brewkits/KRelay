package dev.brewkits.krelay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.brewkits.krelay.basic.BasicDemo
import dev.brewkits.krelay.integration.voyager.VoyagerDemo
import dev.brewkits.krelay.integrations.IntegrationsDemo

/**
 * Main App entry point.
 *
 * Now defaults to IntegrationsDemo with REAL implementations.
 * Menu available to switch between demos if needed.
 */
@Composable
fun App() {
    // Default to IntegrationsDemo (REAL implementations only)
    var selectedDemo by remember { mutableStateOf<DemoType?>(DemoType.INTEGRATIONS) }

    when (selectedDemo) {
        DemoType.BASIC -> BasicDemo(onBackClick = { selectedDemo = null })
        // DemoType.VOYAGER -> VoyagerDemo(onBackClick = { selectedDemo = null })
        // Temporarily disabled due to Voyager lifecycle bug
        DemoType.VOYAGER -> {
            // Fallback to menu with message
            DemoSelectionMenu(onDemoSelected = { selectedDemo = it })
        }
        DemoType.INTEGRATIONS -> IntegrationsDemo(onBackClick = { selectedDemo = null })
        null -> DemoSelectionMenu(onDemoSelected = { selectedDemo = it })
    }
}

@Composable
fun DemoSelectionMenu(onDemoSelected: (DemoType) -> Unit) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🚀 KRelay Demo App",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The Native Interop Bridge for KMP",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Basic Demo Card
                DemoCard(
                    title = "📱 Basic Demo",
                    description = "Shows KRelay fundamentals:\n" +
                            "• Toast/Notification dispatch\n" +
                            "• Navigation commands\n" +
                            "• Feature registration",
                    onClick = { onDemoSelected(DemoType.BASIC) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voyager Integration Demo Card (DISABLED)
                DemoCard(
                    title = "🧭 Voyager Integration (TEMPORARILY DISABLED)",
                    description = "⚠️ Disabled due to Voyager lifecycle bug\n\n" +
                            "Known Issue:\n" +
                            "Voyager's AndroidScreenLifecycleOwner has a bug\n" +
                            "that causes crashes on navigation (DESTROYED\n" +
                            "state transition issue).\n\n" +
                            "This is a Voyager library issue, not KRelay.\n" +
                            "Waiting for Voyager fix or will implement\n" +
                            "alternative navigation demo.",
                    onClick = { /* Disabled */ },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    enabled = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Library Integrations Demo Card
                DemoCard(
                    title = "🔌 Library Integrations (DEFAULT)",
                    description = "The Glue Code Standard:\n" +
                            "✅ REAL implementations only\n" +
                            "• Moko Permissions (Permission mgmt)\n" +
                            "• Moko Biometry (Biometric auth)\n" +
                            "• Play Core/StoreKit (In-app review)\n" +
                            "• Peekaboo (Media picking)\n" +
                            "• Clean ViewModel with zero deps!",
                    onClick = { onDemoSelected(DemoType.INTEGRATIONS) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Info text
                Text(
                    text = "Select a demo to see KRelay in action",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DemoCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        enabled = enabled
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tap to explore →",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class DemoType {
    BASIC,
    VOYAGER,
    INTEGRATIONS
}
