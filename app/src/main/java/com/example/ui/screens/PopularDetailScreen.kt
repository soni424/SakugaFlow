package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SakugaPost
import com.example.ui.SakugaViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularDetailScreen(
    viewModel: SakugaViewModel,
    timeframe: String, // "day", "week", "month", "year"
    onBackClick: () -> Unit,
    onPostClick: (SakugaPost) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var posts by remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Date range translation & query trigger
    val rangeTextAndQuery = remember(currentDate, timeframe) {
        val start: LocalDate
        val end: LocalDate = currentDate
        val display: String

        when (timeframe) {
            "day" -> {
                start = currentDate
                display = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
            "week" -> {
                start = currentDate.minusDays(7)
                display = "${start.format(DateTimeFormatter.ISO_LOCAL_DATE)} to ${end.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
            }
            "month" -> {
                start = currentDate.minusDays(30)
                display = "${currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))} (Last 30 Days)"
            }
            "year" -> {
                start = currentDate.minusDays(365)
                display = "${currentDate.format(DateTimeFormatter.ofPattern("yyyy"))} (Last 365 Days)"
            }
            else -> {
                start = currentDate
                display = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
        }

        val queryStr = "date:${start.format(DateTimeFormatter.ISO_LOCAL_DATE)}..${end.format(DateTimeFormatter.ISO_LOCAL_DATE)} order:score"
        Triple(display, queryStr, start)
    }

    val displaySubtitle = rangeTextAndQuery.first
    val queryStr = rangeTextAndQuery.second

    // Fetch popular posts whenever the timeline bounds shifting occurs
    LaunchedEffect(queryStr) {
        isLoading = true
        posts = viewModel.queryPostsSync(queryStr, limit = 25)
        isLoading = false
    }

    // Capture max post ID for relative timeline estimates
    val latestPostId = remember(posts) { posts.map { it.id }.maxOrNull() ?: 245000 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (timeframe) {
                                "day" -> "Daily Leaderboard"
                                "week" -> "Weekly Leaderboard"
                                "month" -> "Monthly Leaderboard"
                                "year" -> "Yearly Leaderboard"
                                else -> "Popular Posts"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = displaySubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("popular_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Quick slider adjustment buttons inside the header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                currentDate = when (timeframe) {
                                    "day" -> currentDate.minusDays(1)
                                    "week" -> currentDate.minusDays(7)
                                    "month" -> currentDate.minusDays(30)
                                    "year" -> currentDate.minusDays(365)
                                    else -> currentDate.minusDays(1)
                                }
                            },
                            modifier = Modifier.testTag("popular_detail_prev")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous date"
                            )
                        }
                        IconButton(
                            onClick = {
                                // Do not allow sliding into the future
                                val nextDate = when (timeframe) {
                                    "day" -> currentDate.plusDays(1)
                                    "week" -> currentDate.plusDays(7)
                                    "month" -> currentDate.plusDays(30)
                                    "year" -> currentDate.plusDays(365)
                                    else -> currentDate.plusDays(1)
                                }
                                if (!nextDate.isAfter(LocalDate.now())) {
                                    currentDate = nextDate
                                }
                            },
                            modifier = Modifier.testTag("popular_detail_next"),
                            enabled = !currentDate.isAfter(LocalDate.now().minusDays(1))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next date",
                                tint = if (currentDate.isAfter(LocalDate.now().minusDays(1))) 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (isLoading && posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!isLoading && posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No clips found for this range.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { currentDate = LocalDate.now() }) {
                            Text("Reset to Current Date")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(posts) { post ->
                        PopularDetailCard(
                            post = post,
                            viewModel = viewModel,
                            latestPostId = latestPostId,
                            onClick = { onPostClick(post) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PopularDetailCard(
    post: SakugaPost,
    viewModel: SakugaViewModel,
    latestPostId: Int,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val derivedTitle = remember(post.tags) { derivePostTitle(post.tags) }
    val tagsList = remember(post.tags) { post.tags.split(" ").filter { it.isNotEmpty() } }
    val timeAgoStr = remember(post.id, latestPostId) { formatTimeAgo(post.id, latestPostId) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("popular_detail_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Landscape clip preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f)
            ) {
                AsyncImage(
                    model = post.previewUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            // Descriptive Information layout block
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = derivedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFCD34D),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = post.score.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val fileInfo = if (post.fileExt != null) "${post.fileExt.uppercase()} • ${post.width}x${post.height}" else "${post.width}x${post.height}"
                    Text(
                        text = "UID: ${post.id} • $fileInfo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeAgoStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Beautiful scrolling tags list styled color-by-color
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tagsList.take(8).forEach { tag ->
                        val parsed = viewModel.getTagCategoryAndInfo(tag)
                        val category = parsed.first
                        val colorVal = if (isDark) category.darkColorHex else category.lightColorHex
                        val categoryColor = Color(colorVal)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(categoryColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag.replace("_", " "),
                                color = categoryColor,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
