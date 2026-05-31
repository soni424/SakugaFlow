package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import android.view.LayoutInflater
import com.example.R
import com.example.ui.TimelineSegment
import androidx.compose.ui.text.style.TextOverflow

enum class OverlayPosition(val label: String, val alignment: Alignment) {
    TOP_LEFT("Top-Left", Alignment.TopStart),
    TOP_CENTER("Top-Center", Alignment.TopCenter),
    TOP_RIGHT("Top-Right", Alignment.TopEnd),
    CENTER_LEFT("Center-Left", Alignment.CenterStart),
    CENTER_RIGHT("Center-Right", Alignment.CenterEnd),
    BOTTOM_LEFT("Bottom-Left", Alignment.BottomStart),
    BOTTOM_CENTER("Bottom-Center", Alignment.BottomCenter),
    BOTTOM_RIGHT("Bottom-Right", Alignment.BottomEnd)
}

fun formatOverlayText(text: String): String {
    val words = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    val wordLimited = if (words.size > 5) words.take(5).joinToString(" ") + "..." else text
    return if (wordLimited.length > 30) wordLimited.take(27) + "..." else wordLimited
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    currentArtist: String,
    seekToTriggerMs: Long?,
    onSeekConsumed: () -> Unit,
    onPositionChanged: (Long) -> Unit,
    timelineSegments: List<TimelineSegment> = emptyList(),
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
    var showArtistOverlay by remember { mutableStateOf(false) }
    var overlayPosition by remember { mutableStateOf(OverlayPosition.TOP_RIGHT) }

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
                        LayoutInflater.from(ctx).inflate(R.layout.custom_player_view, null) as PlayerView
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

                // Floating Artist Overlay
                if (showArtistOverlay && currentArtist.isNotEmpty()) {
                    val formatted = formatOverlayText(currentArtist)
                    Box(
                        modifier = Modifier
                            .align(overlayPosition.alignment)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatted,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentAlignment = Alignment.CenterStart
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
                        modifier = Modifier.fillMaxSize()
                    )

                    // Render dynamic markers from comments on the timeline
                    if (durationMs > 0) {
                        timelineSegments.filter { it.startMs > 0 && it.startMs <= durationMs }.forEach { segment ->
                            val ratio = segment.startMs.toFloat() / durationMs.toFloat()
                            val offset = (ratio * this.maxWidth.value).dp

                            var showTooltip by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .offset(x = (offset - 6.dp).coerceAtLeast(0.dp))
                                    .size(12.dp)
                                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                    .clickable {
                                        exoPlayer.seekTo(segment.startMs)
                                        currentPositionMs = segment.startMs
                                        showTooltip = !showTooltip
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }

                            if (showTooltip) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = (offset - 50.dp).coerceAtLeast(0.dp), y = (-36).dp)
                                        .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .widthIn(max = 140.dp)
                                ) {
                                    Text(
                                        text = segment.label,
                                        color = MaterialTheme.colorScheme.inverseOnSurface,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

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
                        val frameTime = (1000.0 / selectedFps).toLong()
                        // Triple <<< : Move backward exactly 3 frames
                        MiniNavButton(label = "<<<") {
                            val seekPos = (currentPositionMs - (frameTime * 3)).coerceAtLeast(0)
                            exoPlayer.seekTo(seekPos)
                        }
                        // Double << : Move backward exactly 2 frames
                        MiniNavButton(label = "<<" ) {
                            val seekPos = (currentPositionMs - (frameTime * 2)).coerceAtLeast(0)
                            exoPlayer.seekTo(seekPos)
                        }
                        // Single < : Move backward exactly 1 frame
                        MiniNavButton(label = "<") {
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
                        val frameTime = (1000.0 / selectedFps).toLong()
                        // Single > : Move forward exactly 1 frame
                        MiniNavButton(label = ">") {
                            val seekPos = (currentPositionMs + frameTime).coerceAtMost(durationMs)
                            exoPlayer.seekTo(seekPos)
                        }
                        // Double >> : Move forward exactly 2 frames
                        MiniNavButton(label = ">>") {
                            val seekPos = (currentPositionMs + (frameTime * 2)).coerceAtMost(durationMs)
                            exoPlayer.seekTo(seekPos)
                        }
                        // Triple >>> : Move forward exactly 3 frames
                        MiniNavButton(label = ">>>") {
                            val seekPos = (currentPositionMs + (frameTime * 3)).coerceAtMost(durationMs)
                            exoPlayer.seekTo(seekPos)
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
                            "Advanced Settings",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // "Show Artist Overlay" toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Artist Overlay", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = showArtistOverlay,
                                onCheckedChange = { showArtistOverlay = it }
                            )
                        }

                        // Grid position selector for overlay
                        if (showArtistOverlay) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Artist Overlay Position", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(6.dp)
                                    ) {
                                        val grid = listOf(
                                            listOf(OverlayPosition.TOP_LEFT, OverlayPosition.TOP_CENTER, OverlayPosition.TOP_RIGHT),
                                            listOf(OverlayPosition.CENTER_LEFT, null, OverlayPosition.CENTER_RIGHT),
                                            listOf(OverlayPosition.BOTTOM_LEFT, OverlayPosition.BOTTOM_CENTER, OverlayPosition.BOTTOM_RIGHT)
                                        )
                                        grid.forEach { row ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                row.forEach { pos ->
                                                    if (pos != null) {
                                                        val isSelected = overlayPosition == pos
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                                                )
                                                                .clickable { overlayPosition = pos },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .background(
                                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                        CircleShape
                                                                    )
                                                            )
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.size(28.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .background(Color.Transparent, CircleShape)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = overlayPosition.label,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

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
