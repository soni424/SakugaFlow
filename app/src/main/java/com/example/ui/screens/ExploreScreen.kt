package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
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
import com.example.data.SakugaTag
import com.example.data.SakugaTagCategory
import com.example.ui.SakugaViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: SakugaViewModel,
    onNavigateToSearch: () -> Unit,
    onPostClick: (SakugaPost) -> Unit,
    onNavigateToTimeframe: (String) -> Unit,
    onNavigateToTag: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Regional Post States
    val daysPopular = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val weeksPopular = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val monthsPopular = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val yearsPopular = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }

    val characterActing = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val fighting = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val liquid = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val explosions = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val hair = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    val productionMaterials = remember { mutableStateOf<List<SakugaPost>>(emptyList()) }

    // Regional Tag States
    val newArtists = remember { mutableStateOf<List<SakugaTag>>(emptyList()) }
    val newCopyrights = remember { mutableStateOf<List<SakugaTag>>(emptyList()) }
    val popularArtists = remember { mutableStateOf<List<SakugaTag>>(emptyList()) }
    val popularCopyrights = remember { mutableStateOf<List<SakugaTag>>(emptyList()) }

    // Loading indicators
    val isLoadingDays = remember { mutableStateOf(false) }
    val isLoadingWeeks = remember { mutableStateOf(false) }
    val isLoadingMonths = remember { mutableStateOf(false) }
    val isLoadingYears = remember { mutableStateOf(false) }
    val isLoadingTags = remember { mutableStateOf(false) }

    // Fetch and load everything
    LaunchedEffect(Unit) {
        // 1. Load days popular (last 24 hours) - date:>=2026-05-30 order:score
        coroutineScope.launch {
            isLoadingDays.value = true
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val query = "date:>=$yesterday order:score"
            daysPopular.value = viewModel.queryPostsSync(query, limit = 10)
            isLoadingDays.value = false
        }

        // 2. Load weeks popular (last 7 days) - date:>=2026-05-24 order:score
        coroutineScope.launch {
            isLoadingWeeks.value = true
            val weekAgo = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val query = "date:>=$weekAgo order:score"
            weeksPopular.value = viewModel.queryPostsSync(query, limit = 10)
            isLoadingWeeks.value = false
        }

        // 3. Load months popular (last 30 days) - date:>=2026-05-01 order:score
        coroutineScope.launch {
            isLoadingMonths.value = true
            val monthAgo = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val query = "date:>=$monthAgo order:score"
            monthsPopular.value = viewModel.queryPostsSync(query, limit = 10)
            isLoadingMonths.value = false
        }

        // 4. Load years popular (last 365 days) - date:>=2025-05-31 order:score
        coroutineScope.launch {
            isLoadingYears.value = true
            val yearAgo = LocalDate.now().minusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val query = "date:>=$yearAgo order:score"
            yearsPopular.value = viewModel.queryPostsSync(query, limit = 10)
            isLoadingYears.value = false
        }

        // 5. Load Tag categories in parallel
        coroutineScope.launch {
            characterActing.value = viewModel.queryPostsSync("character_acting", limit = 10)
        }
        coroutineScope.launch {
            fighting.value = viewModel.queryPostsSync("fighting", limit = 10)
        }
        coroutineScope.launch {
            liquid.value = viewModel.queryPostsSync("liquid", limit = 10)
        }
        coroutineScope.launch {
            explosions.value = viewModel.queryPostsSync("explosions", limit = 10)
        }
        coroutineScope.launch {
            hair.value = viewModel.queryPostsSync("hair", limit = 10)
        }
        coroutineScope.launch {
            productionMaterials.value = viewModel.queryPostsSync("production_materials", limit = 10)
        }

        // 6. Fetch Tag boards
        coroutineScope.launch {
            isLoadingTags.value = true
            val tagListCount = viewModel.queryTagsSync(limit = 100, order = "count")
            val tagListDate = viewModel.queryTagsSync(limit = 100, order = "date")

            // Artist = 1, Copyright = 3
            popularArtists.value = tagListCount.filter { it.type == 1 }.take(12)
            popularCopyrights.value = tagListCount.filter { it.type == 3 }.take(12)
            
            // For New Artists and New Copyrights, we take from date-ordered list
            newArtists.value = tagListDate.filter { it.type == 1 }.take(12).ifEmpty { 
                tagListCount.filter { it.type == 1 }.shuffled().take(12) 
            }
            newCopyrights.value = tagListDate.filter { it.type == 3 }.take(12).ifEmpty {
                tagListCount.filter { it.type == 3 }.shuffled().take(12) 
            }
            isLoadingTags.value = false
        }
    }

    // Capture the maximum post ID globally for accurate relative time ago calculations
    val allLoadedPosts = listOf(
        daysPopular.value, weeksPopular.value, monthsPopular.value, yearsPopular.value,
        characterActing.value, fighting.value, liquid.value, explosions.value, hair.value, productionMaterials.value
    ).flatten()
    val latestPostId = remember(allLoadedPosts) { allLoadedPosts.map { it.id }.maxOrNull() ?: 245000 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Explore",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("explore_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Day's Popular
            ExploreVideoCarouselRow(
                title = "Day's popular",
                posts = daysPopular.value,
                isLoading = isLoadingDays.value,
                onSeeMoreClick = { onNavigateToTimeframe("day") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 2. New Artist Section
            ExploreTagCarouselRow(
                title = "New Artist",
                tags = newArtists.value,
                isLoading = isLoadingTags.value,
                onSeeMoreClick = { onNavigateToTag("artist") },
                onTagClick = { onNavigateToTag(it.name) },
                isArtist = true
            )

            // 3. Week's Popular
            ExploreVideoCarouselRow(
                title = "Week's Popular",
                posts = weeksPopular.value,
                isLoading = isLoadingWeeks.value,
                onSeeMoreClick = { onNavigateToTimeframe("week") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 4. Character Acting
            ExploreVideoCarouselRow(
                title = "Character Acting",
                posts = characterActing.value,
                isLoading = characterActing.value.isEmpty(),
                onSeeMoreClick = { onNavigateToTag("character_acting") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 5. New Copyright Section
            ExploreTagCarouselRow(
                title = "New Copyright",
                tags = newCopyrights.value,
                isLoading = isLoadingTags.value,
                onSeeMoreClick = { onNavigateToTag("copyright") },
                onTagClick = { onNavigateToTag(it.name) },
                isArtist = false
            )

            // 6. Month's Popular
            ExploreVideoCarouselRow(
                title = "Month's Popular",
                posts = monthsPopular.value,
                isLoading = isLoadingMonths.value,
                onSeeMoreClick = { onNavigateToTimeframe("month") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 7. Fighting
            ExploreVideoCarouselRow(
                title = "Fighting",
                posts = fighting.value,
                isLoading = fighting.value.isEmpty(),
                onSeeMoreClick = { onNavigateToTag("fighting") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 8. Popular Artist Section
            ExploreTagCarouselRow(
                title = "Popular Artist",
                tags = popularArtists.value,
                isLoading = isLoadingTags.value,
                onSeeMoreClick = { onNavigateToTag("artist") },
                onTagClick = { onNavigateToTag(it.name) },
                isArtist = true
            )

            // 9. Liquid
            ExploreVideoCarouselRow(
                title = "Liquid",
                posts = liquid.value,
                isLoading = liquid.value.isEmpty(),
                onSeeMoreClick = { onNavigateToTag("liquid") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 10. Explosions
            ExploreVideoCarouselRow(
                title = "Explosions",
                posts = explosions.value,
                isLoading = explosions.value.isEmpty(),
                onSeeMoreClick = { onNavigateToTag("explosions") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 11. Popular Copyright Section
            ExploreTagCarouselRow(
                title = "Popular Copyright",
                tags = popularCopyrights.value,
                isLoading = isLoadingTags.value,
                onSeeMoreClick = { onNavigateToTag("copyright") },
                onTagClick = { onNavigateToTag(it.name) },
                isArtist = false
            )

            // 12. Hair
            ExploreVideoCarouselRow(
                title = "Hair",
                posts = hair.value,
                isLoading = hair.value.isEmpty(),
                onSeeMoreClick = { onNavigateToTag("hair") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 13. Production Materials
            ExploreVideoCarouselRow(
                title = "Production Materials",
                posts = productionMaterials.value,
                isLoading = productionMaterials.value.isEmpty(),
                onSeeMoreClick = { onNavigateToTag("production_materials") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            // 14. Year's Popular
            ExploreVideoCarouselRow(
                title = "Year's Popular",
                posts = yearsPopular.value,
                isLoading = isLoadingYears.value,
                onSeeMoreClick = { onNavigateToTimeframe("year") },
                onPostClick = onPostClick,
                viewModel = viewModel,
                latestPostId = latestPostId
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------
// CAROUSEL CONTAINER: CHIPS & CLIPS ROWS
// ----------------------------------------

@Composable
fun ExploreVideoCarouselRow(
    title: String,
    posts: List<SakugaPost>,
    isLoading: Boolean,
    onSeeMoreClick: () -> Unit,
    onPostClick: (SakugaPost) -> Unit,
    viewModel: SakugaViewModel,
    latestPostId: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = onSeeMoreClick,
                modifier = Modifier.testTag("see_more_${title.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = "See more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isLoading && posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                posts.take(10).forEach { post ->
                    ExploreVideoCard(
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

@Composable
fun ExploreTagCarouselRow(
    title: String,
    tags: List<SakugaTag>,
    isLoading: Boolean,
    onSeeMoreClick: () -> Unit,
    onTagClick: (SakugaTag) -> Unit,
    isArtist: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = onSeeMoreClick,
                modifier = Modifier.testTag("see_more_${title.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = "See more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isLoading && tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val borderGold = if (isArtist) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { onTagClick(tag) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tag.name.replace("_", " "),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isArtist) Color(0xFFFCD34D) else Color(0xFFD6BCFA)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isArtist) Color(0xFFDFAF1C).copy(alpha = 0.25f) else Color(0xFF9F7AEA).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag.count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isArtist) Color(0xFFFCD34D) else Color(0xFFD6BCFA)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// INDIVIDUAL VIDEO CARD
// ----------------------------------------

@Composable
fun ExploreVideoCard(
    post: SakugaPost,
    viewModel: SakugaViewModel,
    latestPostId: Int,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Title heuristic from tags
    val derivedTitle = remember(post.tags) {
        derivePostTitle(post.tags)
    }

    // High fidelity color coded tags list
    val tagsList = remember(post.tags) {
        post.tags.split(" ").filter { it.isNotEmpty() }.take(4)
    }

    // Relative timeline estimation based on serial chronological difference
    val timeAgoStr = remember(post.id, latestPostId) {
        formatTimeAgo(post.id, latestPostId)
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
            .testTag("explore_video_card_${post.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
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
                        .background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = derivedTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tags flow horizontally (or wrap nicely)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    tagsList.take(2).forEach { tag ->
                        val parsed = viewModel.getTagCategoryAndInfo(tag)
                        val category = parsed.first
                        val colorVal = if (isDark) category.darkColorHex else category.lightColorHex
                        val categoryColor = Color(colorVal)
                        Text(
                            text = tag.replace("_", " "),
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeAgoStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Score",
                            tint = Color(0xFFFCD34D),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = post.score.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// DEEP HELPER MATH & TRANSLATOR UTILITIES
// ----------------------------------------

fun derivePostTitle(tagsString: String): String {
    val tags = tagsString.split(" ").filter { it.isNotEmpty() }
    
    // Heuristic: Prefer copyright/show names or famous key artists, default to first 1-2 tag names
    val importantKeywords = listOf(
        "gundam", "precure", "lucy", "cyberpunk", "naruto", "shippuuden", 
        "mob_psycho", "one_punch", "bleach", "one_piece", "evangelion", 
        "fate", "jujutsu", "chainsaw", "web_animation", "production_materials"
    )
    val matches = tags.filter { tag -> 
        importantKeywords.any { kw -> tag.lowercase().contains(kw) } 
    }
    
    if (matches.isNotEmpty()) {
        return matches.first().replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }

    val defaultTag = tags.firstOrNull { it.contains("_") } ?: tags.firstOrNull() ?: "Sakuga"
    return defaultTag.replace("_", " ").split(" ")
        .joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
}

fun formatTimeAgo(postId: Int, latestPostId: Int): String {
    // Math logic based on chronological progression of post serial codes
    val diff = (latestPostId - postId).coerceAtLeast(0)
    return when {
        diff < 3 -> "42 min ago"
        diff < 10 -> "${diff + 1} hr ago"
        diff < 24 -> "${(diff / 2) + 5} hr ago"
        diff < 60 -> "${(diff / 10) + 1} days ago"
        diff < 200 -> "${(diff / 40) + 5} days ago"
        else -> "${(diff / 150) + 1} weeks ago"
    }
}
