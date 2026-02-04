package dev.brewkits.krelay.superapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.KRelayInstance
import dev.brewkits.krelay.samples.ToastFeature
import dev.brewkits.krelay.dispatch

// Create two separate, isolated KRelay instances - one per module
val ridesKRelay: KRelayInstance = KRelay.create("Rides")
val foodKRelay: KRelayInstance = KRelay.create("Food")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAppDemo(onBackClick: () -> Unit) {
    var ridesClickCount by remember { mutableStateOf(0) }
    var foodClickCount by remember { mutableStateOf(0) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🚀 Super App Demo (v2.0)") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header explaining the demo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Instance Isolation Demo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This demo shows two independent KRelay instances:\n" +
                                    "• ridesKRelay = KRelay.create(\"Rides\")\n" +
                                    "• foodKRelay = KRelay.create(\"Food\")\n\n" +
                                    "Each has isolated registry and queue - no conflicts!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Rides Mini-App
                RidesMiniApp(
                    clickCount = ridesClickCount,
                    onDispatchClick = {
                        ridesClickCount++
                        ridesKRelay.dispatch<ToastFeature> {
                            it.showShort("🚗 Rides Module: Dispatch #$ridesClickCount")
                        }
                    }
                )

                // Food Mini-App
                FoodMiniApp(
                    clickCount = foodClickCount,
                    onDispatchClick = {
                        foodClickCount++
                        foodKRelay.dispatch<ToastFeature> {
                            it.showShort("🍔 Food Module: Dispatch #$foodClickCount")
                        }
                    }
                )

                // Debug Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📊 Instance Debug Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rides Instance:\n" +
                                    "  • Scope: ${ridesKRelay.scopeName}\n" +
                                    "  • Dispatches: $ridesClickCount\n" +
                                    "  • Registered: ${ridesKRelay.getRegisteredFeaturesCount()} features\n\n" +
                                    "Food Instance:\n" +
                                    "  • Scope: ${foodKRelay.scopeName}\n" +
                                    "  • Dispatches: $foodClickCount\n" +
                                    "  • Registered: ${foodKRelay.getRegisteredFeaturesCount()} features",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RidesMiniApp(clickCount: Int, onDispatchClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Rides Module",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Instance: ridesKRelay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDispatchClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🚗 Dispatch Toast (Rides)")
            }

            if (clickCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dispatched: $clickCount times",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FoodMiniApp(clickCount: Int, onDispatchClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Food Module",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Instance: foodKRelay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDispatchClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("🍔 Dispatch Toast (Food)")
            }

            if (clickCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dispatched: $clickCount times",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
