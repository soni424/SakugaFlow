package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateBack: () -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    val post = remember(postId, posts, savedPosts) {
        posts.find { it.id == postId } ?: savedPosts.find { it.id == postId }
    }

    val tagsList = remember(post?.tags) {
        post?.tags?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    // Trigger dynamic fetch for tag counts & metadata when post details are launched
    LaunchedEffect(tagsList) {
        if (tagsList.isNotEmpty()) {
            viewModel.loadTagInfoForTags(tagsList)
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
                        val isSavedFlow = viewModel.isPostSaved(post.id).collectAsState(initial = false)
                        val isSaved by isSavedFlow
                        
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
                Text("Post not found.", color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Media Player Area
                val isVideo = post.fileExt == "mp4" || post.fileExt == "webm"
                val calculatedRatio = if (post.width > 0 && post.height > 0) post.width.toFloat() / post.height.toFloat() else 1.77f
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(calculatedRatio.coerceIn(0.5f, 2.0f))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        VideoPlayer(
                            videoUrl = post.fileUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = post.fileUrl,
                            contentDescription = "Full Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Details & Tag Categorisation Content Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title and Metadata Cards Row
                    Text(
                        text = "Metadata & Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

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

                    if (post.author != null && post.author.isNotEmpty()) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Animator uploader",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Uploaded By",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = post.author,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Categorized Tags Section
                    Text(
                        text = "Categorized Tags (Tap to search)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    val groupedTags = remember(tagsList, viewModel.tagInfoMap.collectAsState().value) {
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
                                            val tagInfoMapState by viewModel.tagInfoMap.collectAsState()
                                            val infoObj = tagInfoMapState[tag.lowercase().trim()]
                                            
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
        }
    }
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
