package dev.brewkits.krelay.integrations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.basic.DemoSection
import dev.brewkits.krelay.samples.*

/**
 * Integrations Demo
 *
 * Shows how KRelay integrates seamlessly with popular KMP libraries:
 * - Moko Permissions (Permission management)
 * - Moko Biometry (Biometric authentication)
 * - Play Core / StoreKit (In-app review & updates)
 * - Peekaboo (Media picking)
 *
 * Key message: "KRelay is The Glue Code Standard for KMP"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsDemo(onBackClick: () -> Unit) {
    // ALWAYS use real implementations - no mock mode
    val useRealImplementations = true

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("KRelay - Real Integrations") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Text("←", style = MaterialTheme.typography.headlineMedium)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
        ) { paddingValues ->
            val viewModel = remember { IntegrationsViewModel() }

            // ALWAYS use REAL library implementations
            SetupRealIntegrations()

            // Old mock implementation code removed
            if (false) {
                LaunchedEffect(Unit) {
                println("\n╔════════════════════════════════════════════════════════════════╗")
                println("║  🔌 INTEGRATIONS DEMO - Library Integration Setup            ║")
                println("╚════════════════════════════════════════════════════════════════╝")
                println("\n🔧 [IntegrationsDemo] Registering integration implementations...")
                println("   → This demo shows KRelay as 'The Glue Code Standard'")
                println("   → Demonstrating clean integration with:")
                println("     • Moko Permissions (Permission management)")
                println("     • Moko Biometry (Biometric auth)")
                println("     • Play Core / StoreKit (In-app review)")
                println("     • Peekaboo (Media picking)")
                println("\n   Registering implementations...")

                KRelay.register<PermissionFeature>(MockPermissionImpl())
                println("   ✓ PermissionFeature -> MockPermissionImpl (simulating Moko)")

                KRelay.register<BiometricFeature>(MockBiometricImpl())
                println("   ✓ BiometricFeature -> MockBiometricImpl (simulating Moko Biometry)")

                KRelay.register<SystemInteractionFeature>(MockSystemInteractionImpl())
                println("   ✓ SystemInteractionFeature -> MockSystemInteractionImpl (Play Core/StoreKit)")

                KRelay.register<MediaFeature>(MockMediaImpl())
                println("   ✓ MediaFeature -> MockMediaImpl (simulating Peekaboo)")

                // Supporting features
                KRelay.register<ToastFeature>(MockToastImpl())
                KRelay.register<HapticFeature>(MockHapticImpl())
                KRelay.register<NavigationFeature>(MockNavigationImpl())
                KRelay.register<AnalyticsFeature>(MockAnalyticsImpl())

                println("\n💡 KEY INSIGHT:")
                println("   ViewModel (IntegrationsViewModel) has ZERO dependencies on:")
                println("   • Moko Permissions PermissionsController")
                println("   • Moko Biometry BiometryManager")
                println("   • Play Core ReviewManager")
                println("   • Peekaboo rememberImagePickerLauncher")
                println("\n   ✅ Result: Clean architecture, no memory leaks, easy testing!")
                println("═══════════════════════════════════════════════════════════════════\n")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔌 Library Integrations",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "The Glue Code Standard for KMP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ Using REAL Platform Libraries",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Integration 1: Moko Permissions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📸 Permission Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Integration: Moko Permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Problem: PermissionsController needs Activity binding\n" +
                                    "Solution: ViewModel dispatches via KRelay",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                DemoSection(title = "Permission Demos") {
                    Button(
                        onClick = { viewModel.requestCameraPermission() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request Camera Permission")
                    }

                    Button(
                        onClick = { viewModel.takePicture() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Take Picture (with permission check)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Integration 2: Moko Biometry
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔐 Biometric Authentication",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Integration: Moko Biometry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Problem: BiometryManager requires lifecycle\n" +
                                    "Solution: ViewModel decides WHEN, UI decides HOW",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                DemoSection(title = "Biometric Demos") {
                    Button(
                        onClick = { viewModel.authenticateWithBiometrics() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Authenticate with Biometrics")
                    }

                    Button(
                        onClick = { viewModel.confirmPayment(99.99) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Payment (\$99.99)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Integration 3: Play Core / StoreKit
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⭐ In-App Review & Updates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Integration: Play Core / StoreKit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Problem: ReviewManager needs Activity context\n" +
                                    "Solution: Business logic separated from platform API",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                DemoSection(title = "System Interaction Demos") {
                    Button(
                        onClick = { viewModel.requestAppReview() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request In-App Review")
                    }

                    Button(
                        onClick = { viewModel.onOrderCompleted("ORD-12345", 249.99) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Complete Order (triggers review)")
                    }

                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Check for App Updates")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Integration 4: Peekaboo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🖼️ Media Picking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Integration: Peekaboo / ImagePicker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Problem: rememberImagePickerLauncher is Compose-only\n" +
                                    "Solution: ViewModel triggers, UI implements picker",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                DemoSection(title = "Media Demos") {
                    Button(
                        onClick = { viewModel.pickProfilePicture() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick Profile Picture")
                    }

                    Button(
                        onClick = { viewModel.capturePhoto() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Capture Photo (camera + permission)")
                    }

                    Button(
                        onClick = { viewModel.uploadMultiplePhotos() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload Multiple Photos")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Complex workflow
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🚀 Complex Workflow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Demonstrates coordinating multiple integrations:\n" +
                                    "Permission → Media → Biometric → Navigation",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.completeOnboarding() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Complete Onboarding Flow")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "💡 Using REAL platform libraries:\n" +
                            "• iOS: UIAlertController, UIFeedbackGenerator, Moko, StoreKit\n" +
                            "• Android: Toast, Vibrator, Moko, Play Core\n" +
                            "Check console logs to see actual integration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("← Back to Menu")
                }
            }
        }
    }
}
