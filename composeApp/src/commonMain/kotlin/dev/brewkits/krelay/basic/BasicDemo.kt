package dev.brewkits.krelay.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import dev.brewkits.krelay.samples.DemoViewModel
import dev.brewkits.krelay.samples.ToastFeature
import dev.brewkits.krelay.samples.NotificationBridge
import dev.brewkits.krelay.samples.NavigationFeature
import dev.brewkits.krelay.samples.AnalyticsFeature

/**
 * Platform-specific Toast implementation factory.
 * - Android: Returns AndroidToastImpl (real Android Toast with Context)
 * - iOS: Returns IOSToastImpl (real iOS UIAlertController)
 */
@Composable
expect fun rememberPlatformToastImpl(): ToastFeature

/**
 * Basic KRelay Demo
 *
 * Shows fundamental KRelay features:
 * - Toast/Notification dispatch
 * - Navigation commands
 * - Simple feature registration
 *
 * This demo uses mock implementations (no real navigation library).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicDemo(onBackClick: () -> Unit) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Basic Demo") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Text("←", style = MaterialTheme.typography.headlineMedium)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            val viewModel = remember { DemoViewModel() }
            val toastImpl = rememberPlatformToastImpl()
            // KRelay keeps implementations weakly: hold strong references while this screen is shown
            val notificationImpl = remember { MockNotificationImpl() }
            val navigationImpl = remember { MockNavigationImpl() }
            val analyticsImpl = remember { MockAnalyticsImpl() }

            // Register implementations for Basic Demo
            LaunchedEffect(Unit) {
                println("\n╔════════════════════════════════════════════════════════════════╗")
                println("║  📱 BASIC DEMO - KRelay Feature Setup                        ║")
                println("╚════════════════════════════════════════════════════════════════╝")
                println("\n🔧 [BasicDemo] Registering implementations with KRelay...")
                println("   → Registering ToastFeature -> Platform-specific REAL implementation")
                KRelay.register<ToastFeature>(toastImpl)
                println("   → Registering NotificationBridge -> MockNotificationImpl")
                KRelay.register<NotificationBridge>(notificationImpl)
                println("   → Registering NavigationFeature -> MockNavigationImpl")
                KRelay.register<NavigationFeature>(navigationImpl)
                println("   → Registering AnalyticsFeature -> MockAnalyticsImpl")
                KRelay.register<AnalyticsFeature>(analyticsImpl)
                println("   ✓ All implementations registered!")
                println("\n💡 NOTE:")
                println("   • ToastFeature -> REAL (Android Toast / iOS UIAlertController)")
                println("   • Other features -> Mock implementations (just log to console)")
                println("\n✨ Try clicking the buttons below to see KRelay in action!")
                println("═══════════════════════════════════════════════════════════════════\n")
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
                text = "🚀 KRelay Basic Demo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Safe Dispatch • Sticky Events • Leak-Free",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Toast Feature Demos
            DemoSection(title = "Toast Feature") {
                Button(
                    onClick = { viewModel.onDataLoaded(42) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show Data Loaded Toast")
                }

                Button(
                    onClick = { viewModel.onError("Network timeout") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show Error Toast")
                }
            }

            // Notification Feature Demos
            DemoSection(title = "Notification Feature") {
                Button(
                    onClick = { viewModel.onSyncCompleted(itemsUpdated = 25) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show Sync Complete Notification")
                }
            }

            // Navigation Feature Demos
            DemoSection(title = "Navigation Feature") {
                Button(
                    onClick = { viewModel.onLoginSuccess() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate Login Success")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "💡 Tip: Check Logcat to see KRelay debug logs",
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

@Composable
fun DemoSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
    }
}
