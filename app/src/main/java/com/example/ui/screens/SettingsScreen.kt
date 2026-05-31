package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.SakugaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SakugaViewModel,
    onNavigateBack: () -> Unit
) {
    val quality by viewModel.playbackQuality.collectAsState()
    val autoplay by viewModel.autoplay.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    
    var cacheSize by remember { mutableStateOf(viewModel.getAppCacheSize()) }
    var qualityExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- VIDEO PLAYBACK SECTION ---
            Text(
                text = "Video Playback",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Playback Quality", style = MaterialTheme.typography.bodyLarge)
                            Text("Set preferred stream quality", style = MaterialTheme.typography.labelMedium)
                        }
                        Box {
                            Button(
                                onClick = { qualityExpanded = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(quality)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = qualityExpanded,
                                onDismissRequest = { qualityExpanded = false }
                            ) {
                                listOf("High", "Medium", "Low").forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q) },
                                        onClick = {
                                            viewModel.updatePlaybackQuality(q)
                                            qualityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Autoplay Videos", style = MaterialTheme.typography.bodyLarge)
                            Text("Start videos automatically on detail screens", style = MaterialTheme.typography.labelMedium)
                        }
                        Switch(
                            checked = autoplay,
                            onCheckedChange = { viewModel.toggleAutoplay(it) }
                        )
                    }
                }
            }

            // --- SEARCH SETTINGS ---
            Text(
                text = "Search Controls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Search History", style = MaterialTheme.typography.bodyLarge)
                        Text("Clear cached tag completions and search history", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { viewModel.clearSearchHistory() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }

            // --- STORAGE & CACHE ---
            Text(
                text = "Storage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Cache Size", style = MaterialTheme.typography.bodyLarge)
                        Text("Used space: $cacheSize", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = {
                            viewModel.clearAppCache()
                            cacheSize = viewModel.getAppCacheSize()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Cache")
                    }
                }
            }

            // --- THEME ---
            Text(
                text = "UI Customization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Theme Mode", style = MaterialTheme.typography.bodyLarge)
                        Text("Select app shade visual model", style = MaterialTheme.typography.labelMedium)
                    }
                    Box {
                        Button(
                            onClick = { themeExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(themeMode.replaceFirstChar { it.uppercase() })
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            listOf("system", "dark", "light").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.updateThemeMode(mode)
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
