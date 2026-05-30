package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayer(
    videoUrl: String,
    currentArtist: String,
    seekToTriggerMs: Long?,
    onSeekConsumed: () -> Unit,
    onPositionChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var volume by remember { mutableStateOf(1f) }

    // Advanced features from user image
    var playSpeed by remember { mutableStateOf(1.0f) }
    var blurRadius by remember { mutableStateOf(0.0f) }
    var isFlipped by remember { mutableStateOf(false) }
    var selectedFps by remember { mutableStateOf(24.0) }
    var showExtendedSettings by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            repeatMode = Player.REPEAT_MODE_ONE // Sakuga loops are best played on repeat!
            prepare()
            playWhenReady = true
        }
    }

    // Handles external seek trigger from comments/timestamps
    LaunchedEffect(seekToTriggerMs, exoPlayer) {
        if (seekToTriggerMs != null) {
            try {
                exoPlayer.seekTo(seekToTriggerMs)
                currentPositionMs = seekToTriggerMs
                onPositionChanged(seekToTriggerMs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onSeekConsumed()
        }
    }

    // React to speed change
    LaunchedEffect(playSpeed, exoPlayer) {
        try {
            val params = exoPlayer.playbackParameters.withSpeed(playSpeed)
            exoPlayer.playbackParameters = params
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer.stop()
            } catch (e: Exception) {
                // Ignore
            }
            exoPlayer.release()
        }
    }

    // Update state continuously while video is playing
    LaunchedEffect(exoPlayer) {
        try {
            while (true) {
                currentPositionMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                isPlaying = exoPlayer.isPlaying
                onPositionChanged(currentPositionMs)
                delay(100)
            }
        } catch (e: Exception) {
            // Player might be released or thread cancelled, safely catch to prevent crashing
        }
    }

    // Time calculations
    val formattedCurrentTime = remember(currentPositionMs) { formatTime(currentPositionMs) }
    val formattedDuration = remember(durationMs) { formatTime(durationMs) }
    val currentFrame = remember(currentPositionMs, selectedFps) {
        (currentPositionMs * selectedFps / 1000.0).toInt()
    }
    val totalFrames = remember(durationMs, selectedFps) {
        (durationMs * selectedFps / 1000.0).toInt().coerceAtLeast(1)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // Video Frame view with interactive scale and blur effects
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false // Disable standard default controllers
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != exoPlayer) {
                            playerView.player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (isFlipped) -1f else 1f
                        }
                        .then(
                            if (blurRadius > 0.1f) {
                                Modifier.blur(blurRadius.dp)
                            } else {
                                Modifier
                            }
                        )
                )

                // Quick Play/Pause overlay indicators
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable { exoPlayer.play() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Paused",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // Custom Timeline and play slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Slider(
                    value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                    onValueChange = { percent ->
                        val target = (percent * durationMs).toLong()
                        exoPlayer.seekTo(target)
                        currentPositionMs = target
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                // Playback row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            volume = if (volume > 0f) 0f else 1f
                            exoPlayer.volume = volume
                        }) {
                            Icon(
                                imageVector = if (volume > 0f) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Mute Toggle",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "$formattedCurrentTime / $formattedDuration",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(playSpeed)}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        IconButton(onClick = { showExtendedSettings = !showExtendedSettings }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Toggle Settings Panel",
                                tint = if (showExtendedSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Image-based Booru navigation deck: <<<  <<  <  [Frame Info]  >  >>  >>>
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // <<< Go to start (Seek to 0)
                        MiniNavButton(label = "<<<") {
                            exoPlayer.seekTo(0)
                        }
                        // << Seek backward 1 second
                        MiniNavButton(label = "<<") {
                            val seekPos = (currentPositionMs - 1000).coerceAtLeast(0)
                            exoPlayer.seekTo(seekPos)
                        }
                        // < Move single frame backward
                        MiniNavButton(label = "<") {
                            val frameTime = (1000.0 / selectedFps).toLong()
                            val seekPos = (currentPositionMs - frameTime).coerceAtLeast(0)
                            exoPlayer.seekTo(seekPos)
                        }
                    }

                    // Dynamic Frame info display (Matches precisely the image style!)
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .padding(vertical = 4.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$currentFrame / $totalFrames",
                                color = Color(0xFF4CAF50), // Vibrant neon green frame counter
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$formattedCurrentTime / $formattedDuration",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // > Move single frame forward
                        MiniNavButton(label = ">") {
                            val frameTime = (1000.0 / selectedFps).toLong()
                            val seekPos = (currentPositionMs + frameTime).coerceAtMost(durationMs)
                            exoPlayer.seekTo(seekPos)
                        }
                        // >> Seek forward 1 second
                        MiniNavButton(label = ">>") {
                            val seekPos = (currentPositionMs + 1000).coerceAtMost(durationMs)
                            exoPlayer.seekTo(seekPos)
                        }
                        // >>> Go to end
                        MiniNavButton(label = ">>>") {
                            exoPlayer.seekTo(durationMs)
                        }
                    }
                }

                // Dynamic Current Artist and metadata row shown in player
                if (currentArtist.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Animator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Current artist: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentArtist,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Collapsible sidebar-like configuration panel matching the uploaded image!
                if (showExtendedSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Advanced Frame Settings",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Video speed slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Video speed (${"%.2f".format(playSpeed)}x)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                TextButton(onClick = { playSpeed = 1.0f }) {
                                    Text("Reset (1x)", fontSize = 10.sp)
                                }
                            }
                            Slider(
                                value = playSpeed,
                                onValueChange = { playSpeed = it },
                                valueRange = 0.1f..2.0f,
                                modifier = Modifier.height(18.dp)
                            )
                        }

                        // 2. Graphic filter (Blur radius slider)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Filter (Blur / ${blurRadius.toInt()}px)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                TextButton(onClick = { blurRadius = 0.0f }) {
                                    Text("Clear blur", fontSize = 10.sp)
                                }
                            }
                            Slider(
                                value = blurRadius,
                                onValueChange = { blurRadius = it },
                                valueRange = 0.0f..25.0f,
                                modifier = Modifier.height(18.dp)
                            )
                        }

                        // 3. Mirror & Frame rates side-by-side controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Toggle horizontal flip
                            Button(
                                onClick = { isFlipped = !isFlipped },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFlipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flip,
                                    contentDescription = "Flip horizontally",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isFlipped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Flip Horizontally",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFlipped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Dynamic framerate selector
                            var fpsExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(0.9f)) {
                                OutlinedCard(
                                    onClick = { fpsExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${selectedFps.toInt()} FPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = fpsExpanded,
                                    onDismissRequest = { fpsExpanded = false }
                                ) {
                                    listOf(23.976, 24.0, 25.0, 29.97, 30.0, 60.0).forEach { rate ->
                                        DropdownMenuItem(
                                            text = { Text("$rate FPS", fontSize = 12.sp) },
                                            onClick = {
                                                selectedFps = rate
                                                fpsExpanded = false
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
    }
}

@Composable
fun MiniNavButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Converts millisecond timestamp to minutes:seconds.millis format (e.g. 0:06.1)
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenthsOfSecond = (ms % 1000) / 100
    return "$minutes:${String.format("%02d", seconds)}.$tenthsOfSecond"
}
