package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPostsScreen(
    viewModel: SakugaViewModel,
    tag: String,
    onBackClick: () -> Unit,
    onPostClick: (SakugaPost) -> Unit
) {
    var posts by remember { mutableStateOf<List<SakugaPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var isEndReached by remember { mutableStateOf(false) }

    // Translate tag to nice header display title
    val headingTitle = remember(tag) {
        tag.split(":").last().replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    // Load initial page of posts
    LaunchedEffect(tag) {
        isLoading = true
        page = 1
        isEndReached = false
        val results = viewModel.queryPostsSync(tag, page = 1, limit = 20)
        posts = results
        if (results.isEmpty()) {
            isEndReached = true
        }
        isLoading = false
    }

    val coroutineScope = rememberCoroutineScope()

    // Load more entries for pagination
    fun loadNextPage() {
        if (isLoading || isEndReached) return
        isLoading = true
        coroutineScope.launch {
            val nextPage = page + 1
            val results = viewModel.queryPostsSync(tag, page = nextPage, limit = 20)
            if (results.isEmpty()) {
                isEndReached = true
            } else {
                page = nextPage
                posts = posts + results
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth().padding(end = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = headingTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Tag: $tag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("tag_posts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                    Text(
                        text = "No clips listed for '$tag'.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(posts) { index, post ->
                        if (index >= posts.size - 4 && !isLoading && !isEndReached) {
                            LaunchedEffect(index) {
                                loadNextPage()
                            }
                        }
                        TagPostGridCard(
                            post = post,
                            viewModel = viewModel,
                            onClick = { onPostClick(post) }
                        )
                    }

                    if (isLoading && posts.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagPostGridCard(
    post: SakugaPost,
    viewModel: SakugaViewModel,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tag_post_grid_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
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
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                val tagsList = remember(post.tags) {
                    post.tags.split(" ").filter { it.isNotEmpty() }.take(2)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    tagsList.forEach { tag ->
                        val parsed = viewModel.getTagCategoryAndInfo(tag)
                        val category = parsed.first
                        val colorVal = if (isDark) category.darkColorHex else category.lightColorHex
                        val categoryColor = Color(colorVal)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(categoryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag.replace("_", " "),
                                color = categoryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Score: ${post.score}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ID: ${post.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
