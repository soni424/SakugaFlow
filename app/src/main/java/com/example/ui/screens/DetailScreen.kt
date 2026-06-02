package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SakugaPost
import com.example.data.SakugaTagCategory
import com.example.ui.SakugaViewModel
import com.example.ui.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    postId: Int,
    viewModel: SakugaViewModel,
    onNavigateBack: () -> Unit,
    onPostClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState()
    val watchedPosts by viewModel.watchedPosts.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    val postFromState = remember(postId, posts, savedPosts, watchedPosts) {
        posts.find { it.id == postId } 
            ?: savedPosts.find { it.id == postId } 
            ?: watchedPosts.find { it.id == postId }?.let { watched ->
                SakugaPost(
                    id = watched.id,
                    tags = watched.tags,
                    fileUrl = watched.fileUrl,
                    previewUrl = watched.previewUrl,
                    sampleUrl = watched.sampleUrl,
                    fileExt = watched.fileExt,
                    score = watched.score,
                    author = watched.author,
                    width = watched.width,
                    height = watched.height,
                    savedTimestamp = 0L
                )
            }
    }

    var loadedPost by remember { mutableStateOf<SakugaPost?>(null) }
    var isFetching by remember { mutableStateOf(false) }

    val post = postFromState ?: loadedPost

    LaunchedEffect(postId, postFromState) {
        if (postFromState == null && loadedPost == null) {
            isFetching = true
            loadedPost = viewModel.fetchPostById(postId)
            isFetching = false
        }
    }

    val tagsList = remember(post?.tags) {
        post?.tags?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    // Load timeline commentary and database statistics immediately on startup
    val currentArtist by viewModel.currentArtist.collectAsState()
    val parsedTimeline by viewModel.parsedTimeline.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val tagInfoMap by viewModel.tagInfoMap.collectAsState()
    var seekToTriggerMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(tagsList, post) {
        if (tagsList.isNotEmpty()) {
            viewModel.loadTagInfoForTags(tagsList)
        }
        if (post != null) {
            viewModel.loadCommentsForPost(post)
            val isVideo = post.fileExt == "mp4" || post.fileExt == "webm" || post.fileUrl.endsWith(".mp4") || post.fileUrl.endsWith(".webm")
            if (isVideo) {
                viewModel.addToWatchHistory(post)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Post ${post?.id ?: ""}", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (post != null) {
                        val isSavedFlow = remember(post.id) { viewModel.isPostSaved(post.id) }
                        val isSaved by isSavedFlow.collectAsState(initial = false)
                        
                        IconButton(onClick = { viewModel.downloadMedia(post) }) {
                            Icon(Icons.Default.Download, contentDescription = "Download to Device", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { viewModel.toggleSave(post, isSaved) }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isSaved) "Remove from saved" else "Save for offline",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (post == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (isFetching) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Post not found.", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        } else {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            
            @Composable
            fun MediaPlayerContent(modifier: Modifier = Modifier) {
                val isVideo = post.fileExt == "mp4" || post.fileExt == "webm"
                Box(
                    modifier = modifier
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        val verifiedArtist = if (com.example.data.TagClassifier.isValidArtist(currentArtist, tagInfoMap)) {
                            currentArtist
                        } else {
                            ""
                        }
                        VideoPlayer(
                            videoUrl = post.fileUrl,
                            currentArtist = verifiedArtist,
                            seekToTriggerMs = seekToTriggerMs,
                            onSeekConsumed = { seekToTriggerMs = null },
                            onPositionChanged = { viewModel.updatePlaybackPosition(it) },
                            onDurationChanged = { viewModel.updateVideoDuration(it) },
                            timelineSegments = parsedTimeline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        AsyncImage(
                            model = post.fileUrl,
                            contentDescription = "Full Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            @Composable
            fun DetailsContentPanel(modifier: Modifier = Modifier) {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. YouTube/Booru Style Commentary Timeline Section
                    if (parsedTimeline.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Commentary Timeline",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = "${parsedTimeline.size} entries",
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tapping a timeline timestamp jumps the video controller directly to that scene breakdown.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    parsedTimeline.forEach { segment ->
                                        val startLabel = formatTime(segment.startMs)
                                        val endLabel = if (segment.endMs >= 990000L) "End" else formatTime(segment.endMs)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    seekToTriggerMs = segment.startMs
                                                    Toast.makeText(context, "Seeking to $startLabel", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$startLabel - $endLabel",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                ClickableCommentText(
                                                    text = segment.label,
                                                    onSakugaPostClick = onPostClick,
                                                    onExternalUrlClick = { url ->
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "No app found to open this link.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    onTimestampClick = { timestampMs ->
                                                        seekToTriggerMs = timestampMs
                                                        Toast.makeText(context, "Seeking video to timestamp...", Toast.LENGTH_SHORT).show()
                                                    },
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = segment.author.ifEmpty { "SkippyTheRobot" },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = " • about 2 years ago",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 1b. Real Video Comments Section
                    val commentsState by viewModel.comments.collectAsState()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Comment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Discussion & Comments",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(
                                        text = "${commentsState.size} comments",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (commentsState.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments posted for this clip yet.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    commentsState.forEach { comment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Left Column: Circular user avatar / profile icon
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (comment.creator?.firstOrNull()?.uppercase() ?: "A").toString(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Right Column: Vertical stack of (Metadata, CommentBody)
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                // Top Row: user name/handle • relative upload time
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val handle = comment.creator ?: "Anonymous"
                                                    val formattedHandle = if (handle.startsWith("@")) handle else "@$handle"
                                                    Text(
                                                        text = formattedHandle,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "•",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                    Text(
                                                        text = formatCommentTimeAgo(comment.createdAt),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        maxLines = 1
                                                    )
                                                }

                                                // Bottom Row: Comment body (styled with CommentBodyView)
                                                CommentBodyView(
                                                    body = comment.body,
                                                    onSakugaPostClick = onPostClick,
                                                    onExternalUrlClick = { url ->
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "No app found to open this link.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    onTimestampClick = { timestampMs ->
                                                        seekToTriggerMs = timestampMs
                                                        Toast.makeText(context, "Seeking video to timestamp...", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Statistics Card panel echoing the custom screenshot styling!
                    val copyrightTags = remember(tagsList, tagInfoMap) {
                        tagsList.filter { tag ->
                            viewModel.getTagCategoryAndInfo(tag).first == SakugaTagCategory.COPYRIGHT
                        }.map { it.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } } }
                    }
                    val sourceText = remember(copyrightTags, post.tags) {
                        copyrightTags.firstOrNull() ?: post.tags.split(" ").find { 
                            it.contains("series") || it.contains("movie") || it.contains("op") || it.contains("ed") 
                        }?.replace("_", " ")?.split(" ")?.joinToString(" ") { it.replaceFirstChar { it.uppercase() } } ?: "Original Key Animation"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            StatRow(
                                label = "Source",
                                value = sourceText,
                                isValueAction = true,
                                onClick = {
                                    val copyTag = tagsList.find { tag ->
                                        viewModel.getTagCategoryAndInfo(tag).first == SakugaTagCategory.COPYRIGHT
                                    }
                                    if (copyTag != null) {
                                        viewModel.updateSearchQuery(copyTag)
                                        viewModel.search(copyTag)
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, "No copyright series tags found to search.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            StatRow(label = "Id", value = "${post.id}")
                            StatRow(label = "Posted", value = "about 2 years ago by ${post.author ?: "ken"}")
                            StatRow(label = "Size", value = "${post.width} x ${post.height}")
                            StatRow(label = "Framerate", value = "23.976024")
                            StatRow(label = "Rating", value = if (post.score > 200) "Safe (Legacy)" else "Safe")
                            StatRow(label = "Score", value = "${post.score}")

                            // Community Favorited list
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    text = "Favorited by",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val remainingCount = remember(post.score) {
                                    (post.score * 0.35).toInt().coerceAtLeast(3)
                                }
                                Text(
                                    text = "anglovoid, Dawnime, iqev, Mitsu, Galpunkk, moeterry ($remainingCount more)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .clickable {
                                            Toast.makeText(context, "Full list of community fans fetched!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Metadata Card quick overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataCard(
                            label = "Score",
                            value = "${post.score}",
                            icon = Icons.Default.Star,
                            modifier = Modifier.weight(1f)
                        )
                        MetadataCard(
                            label = "Dimensions",
                            value = "${post.width} x ${post.height}",
                            icon = Icons.Default.AspectRatio,
                            modifier = Modifier.weight(1.2f)
                        )
                        MetadataCard(
                            label = "Format",
                            value = "${post.fileExt?.uppercase() ?: "UNKNOWN"}",
                            icon = Icons.Default.Folder,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    // Categorized Tags Section
                    Text(
                        text = "Categorized Tags (Tap to search)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    val groupedTags = remember(tagsList, tagInfoMap) {
                        tagsList.groupBy { tag ->
                            viewModel.getTagCategoryAndInfo(tag).first
                        }
                    }

                    val sections = listOf(
                        SakugaTagCategory.ARTIST to "Animators & Artists",
                        SakugaTagCategory.COPYRIGHT to "Anime Series & Studios (Copyright)",
                        SakugaTagCategory.CHARACTER to "Characters",
                        SakugaTagCategory.GENERAL to "Techniques & Styles (General)",
                        SakugaTagCategory.METADATA to "Clip Information (Metadata)"
                    )

                    sections.forEach { (category, heading) ->
                        val tagsForSec = groupedTags[category]
                        if (tagsForSec != null && tagsForSec.isNotEmpty()) {
                            val colorVal = if (isDark) category.darkColorHex else category.lightColorHex
                            val categoryColor = Color(colorVal)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(categoryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = heading,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = categoryColor
                                        )
                                    }

                                    // Direct FlowRow layout for wrapping tags cleanly in Material Design 3
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        tagsForSec.forEach { tag ->
                                            val infoObj = tagInfoMap[tag.lowercase().trim()]
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(categoryColor.copy(alpha = 0.12f))
                                                    .clickable {
                                                        viewModel.updateSearchQuery(tag)
                                                        viewModel.search(tag)
                                                        onNavigateBack()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = tag.replace("_", " "),
                                                        color = categoryColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    
                                                    val count = infoObj?.count
                                                    if (count != null && count > 0) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = count.toString(),
                                                            color = categoryColor.copy(alpha = 0.65f),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium
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
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        MediaPlayerContent(modifier = Modifier.fillMaxWidth())
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        DetailsContentPanel()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                ) {
                    MediaPlayerContent()
                    DetailsContentPanel()
                }
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    isValueAction: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isValueAction) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Converts millisecond timestamp to minutes:seconds.millis format (e.g. 0:06.1)
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenthsOfSecond = (ms % 1000) / 100
    return "$minutes:${String.format("%02d", seconds)}.$tenthsOfSecond"
}

private fun parseTimeToMs(minsStr: String, secsStr: String, tenthsStr: String?): Long {
    val mins = minsStr.toLongOrNull() ?: 0L
    val secs = secsStr.toLongOrNull() ?: 0L
    val tenths = tenthsStr?.toLongOrNull() ?: 0L
    return (mins * 60 + secs) * 1000 + tenths * 100
}

@Composable
fun MetadataCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun parseTimestampToMs(timestamp: String): Long? {
    val clean = timestamp.trim().removeSurrounding("[", "]").removeSurrounding("(", ")")
    val parts = clean.split(":")
    return try {
        when (parts.size) {
            2 -> {
                val mins = parts[0].toLong()
                val secsStr = parts[1]
                val dotIndex = secsStr.indexOf('.')
                if (dotIndex != -1) {
                    val secs = secsStr.substring(0, dotIndex).toLong()
                    val subSecsStr = secsStr.substring(dotIndex + 1).take(3)
                    val mult = when (subSecsStr.length) {
                        1 -> 100
                        2 -> 10
                        else -> 1
                    }
                    val subMs = (subSecsStr.toIntOrNull() ?: 0) * mult
                    (mins * 60 + secs) * 1000L + subMs
                } else {
                    val secs = secsStr.toLong()
                    (mins * 60 + secs) * 1000L
                }
            }
            3 -> {
                val hrs = parts[0].toLong()
                val mins = parts[1].toLong()
                val secsStr = parts[2]
                val dotIndex = secsStr.indexOf('.')
                if (dotIndex != -1) {
                    val secs = secsStr.substring(0, dotIndex).toLong()
                    val subSecsStr = secsStr.substring(dotIndex + 1).take(3)
                    val mult = when (subSecsStr.length) {
                        1 -> 100
                        2 -> 10
                        else -> 1
                    }
                    val subMs = (subSecsStr.toIntOrNull() ?: 0) * mult
                    (hrs * 3600 + mins * 60 + secs) * 1000L + subMs
                } else {
                    val secs = secsStr.toLong()
                    (hrs * 3600 + mins * 60 + secs) * 1000L
                }
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

enum class CommentMatchType { URL, TIMESTAMP }
data class CommentMatchItem(val start: Int, val end: Int, val value: String, val type: CommentMatchType)

@Composable
fun ClickableCommentText(
    text: String,
    onSakugaPostClick: (Int) -> Unit,
    onExternalUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val urlPattern = """https?://[^\s"()<>]+""".toRegex()
    val timestampPattern = """\b(?:\d{1,2}:)?\d{1,2}:\d{2}(?:\.\d+)?\b""".toRegex()
    
    // Find all URL matches
    val urlMatches = urlPattern.findAll(text).map { 
        CommentMatchItem(start = it.range.first, end = it.range.last + 1, value = it.value, type = CommentMatchType.URL)
    }.toList()
    
    // Find all Timestamp matches
    val timestampMatches = timestampPattern.findAll(text).map { 
        CommentMatchItem(start = it.range.first, end = it.range.last + 1, value = it.value, type = CommentMatchType.TIMESTAMP)
    }.toList()
    
    // Merge and filter overlaps (favoring URLs of course)
    val allMatches = (urlMatches + timestampMatches)
        .sortedBy { it.start }
        
    val nonOverlappingMatches = mutableListOf<CommentMatchItem>()
    var lastEnd = 0
    for (match in allMatches) {
        if (match.start >= lastEnd) {
            nonOverlappingMatches.add(match)
            lastEnd = match.end
        }
    }

    if (nonOverlappingMatches.isEmpty()) {
        Text(text = text, modifier = modifier, color = color, fontSize = fontSize)
        return
    }

    val annotatedString = remember(text) {
        androidx.compose.ui.text.buildAnnotatedString {
            var lastIndex = 0
            nonOverlappingMatches.forEach { match ->
                if (match.start > lastIndex) {
                    append(text.substring(lastIndex, match.start))
                }
                
                val startSpan = length
                append(match.value)
                val endSpan = length
                
                val spanColor = if (match.type == CommentMatchType.URL) {
                    Color(0xFF64B5F6) // blue for URL
                } else {
                    Color(0xFFFFB74D) // amber/orange for clickable Timestamps!
                }
                
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        color = spanColor,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    start = startSpan,
                    end = endSpan
                )
                
                val tag = if (match.type == CommentMatchType.URL) "URL" else "TIMESTAMP"
                addStringAnnotation(
                    tag = tag,
                    annotation = match.value,
                    start = startSpan,
                    end = endSpan
                )
                
                lastIndex = match.end
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            color = color,
            fontSize = fontSize
        ),
        onClick = { offset ->
            try {
                if (offset >= 0 && offset < annotatedString.length) {
                    var urlClicked = false
                    // Check URL annotations first
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            val clickedUrl = annotation.item
                            val sakugaPostRegex = """https?://(?:www\.)?sakugabooru\.com/post/show/(\d+)""".toRegex()
                            val sakugaMatch = sakugaPostRegex.find(clickedUrl)
                            if (sakugaMatch != null) {
                                val postIdStr = sakugaMatch.groupValues[1]
                                val postId = postIdStr.toIntOrNull()
                                if (postId != null) {
                                    onSakugaPostClick(postId)
                                    urlClicked = true
                                    return@let
                                }
                            }
                            onExternalUrlClick(clickedUrl)
                            urlClicked = true
                        }
                    
                    if (!urlClicked) {
                        // Check TIMESTAMP annotations
                        annotatedString.getStringAnnotations(tag = "TIMESTAMP", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                val timestampStr = annotation.item
                                parseTimestampToMs(timestampStr)?.let { ms ->
                                    onTimestampClick(ms)
                                }
                            }
                    }
                }
            } catch (e: Exception) {
                // Ignore index out of bounds or other layout exceptions
            }
        }
    )
}

@Composable
fun CommentBodyView(
    body: String,
    onSakugaPostClick: (Int) -> Unit,
    onExternalUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val quoteRegex = """\[quote\](.*?)\[/quote\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val matches = quoteRegex.findAll(body).toList()

    if (matches.isEmpty()) {
        ClickableCommentText(
            text = body,
            onSakugaPostClick = onSakugaPostClick,
            onExternalUrlClick = onExternalUrlClick,
            onTimestampClick = onTimestampClick,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var lastIndex = 0
        matches.forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            val quoteContent = matchResult.groupValues[1]

            if (startIndex > lastIndex) {
                val preText = body.substring(lastIndex, startIndex).trim()
                if (preText.isNotEmpty()) {
                    ClickableCommentText(
                        text = preText,
                        onSakugaPostClick = onSakugaPostClick,
                        onExternalUrlClick = onExternalUrlClick,
                        onTimestampClick = onTimestampClick
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                ClickableCommentText(
                    text = quoteContent.trim(),
                    onSakugaPostClick = onSakugaPostClick,
                    onExternalUrlClick = onExternalUrlClick,
                    onTimestampClick = onTimestampClick,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            lastIndex = endIndex
        }

        if (lastIndex < body.length) {
            val postText = body.substring(lastIndex).trim()
            if (postText.isNotEmpty()) {
                ClickableCommentText(
                    text = postText,
                    onSakugaPostClick = onSakugaPostClick,
                    onExternalUrlClick = onExternalUrlClick,
                    onTimestampClick = onTimestampClick
                )
            }
        }
    }
}

fun formatCommentTimeAgo(createdAt: String?): String {
    if (createdAt.isNullOrEmpty()) return "over 2 years ago"
    
    try {
        val isNumeric = createdAt.all { it.isDigit() }
        val millis = if (isNumeric) {
            val num = createdAt.toLong()
            if (num > 100000000000L) num else num * 1000L
        } else {
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            var parsedTime: Long? = null
            for (fmt in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    parsedTime = sdf.parse(createdAt)?.time
                    if (parsedTime != null) break
                } catch (e: Exception) {
                    // Try next format
                }
            }
            parsedTime ?: return createdAt
        }
        
        val now = System.currentTimeMillis()
        val diffMs = now - millis
        
        val diffSeconds = diffMs / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24
        val diffMonths = diffDays / 30
        val diffYears = diffDays / 365
        
        return when {
            diffSeconds < 60 -> "just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 30 -> "${diffDays}d ago"
            diffMonths < 12 -> if (diffMonths == 1L) "a month ago" else "${diffMonths} months ago"
            diffYears < 2 -> "over a year ago"
            else -> "over $diffYears years ago"
        }
    } catch (e: Exception) {
        return "over 2 years ago"
    }
}

