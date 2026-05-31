package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SakugaPost
import androidx.compose.ui.platform.testTag
import com.example.ui.SakugaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SakugaViewModel,
    onNavigateBack: () -> Unit,
    onPostClick: (SakugaPost) -> Unit
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

            // --- WATCH HISTORY SECTION ---
            val watchedList by viewModel.watchedPosts.collectAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_history_section"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Watch History",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (watchedList.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearWatchedHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All Watch History", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (watchedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .testTag("no_history_text"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No watched videos yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp)
                            .testTag("watch_history_row"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        watchedList.forEach { watched ->
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        val post = SakugaPost(
                                            id = watched.id,
                                            tags = watched.tags,
                                            fileUrl = watched.fileUrl,
                                            previewUrl = watched.previewUrl,
                                            sampleUrl = watched.sampleUrl,
                                            fileExt = watched.fileExt,
                                            score = watched.score,
                                            author = watched.author,
                                            width = watched.width,
                                            height = watched.height
                                        )
                                        onPostClick(post)
                                    }
                                    .testTag("watch_item_card_${watched.id}")
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(85.dp)
                                    ) {
                                        AsyncImage(
                                            model = watched.previewUrl,
                                            contentDescription = "Preview for ${watched.id}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Play Icon Overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = "Play",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // Tiny Close/Delete button to delete this history item
                                        IconButton(
                                            onClick = { viewModel.removeWatchedPost(watched.id) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .testTag("remove_watch_item_${watched.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove from history",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(6.dp)) {
                                        val firstTag = remember(watched.tags) {
                                            watched.tags.split(" ").firstOrNull { it.isNotEmpty() }?.replace("_", " ") ?: "Video"
                                        }
                                        Text(
                                            text = "ID: ${watched.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = firstTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
