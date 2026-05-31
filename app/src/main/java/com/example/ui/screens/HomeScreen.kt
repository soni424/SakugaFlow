package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SakugaPost
import com.example.ui.SakugaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SakugaViewModel,
    onNavigateToSaved: () -> Unit,
    onPostClick: (SakugaPost) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var expandedFilters by remember { mutableStateOf(false) }

    val sortOptions = listOf(
        "Date" to "date",
        "Source" to "source",
        "Source Asc" to "source_asc",
        "Post ID" to "id",
        "ID Desc" to "id_desc",
        "Score" to "score",
        "Score Asc" to "score_asc",
        "Size MP" to "mpixels",
        "Size MP Asc" to "mpixels_asc",
        "Random" to "random"
    )

    val ratingOptions = listOf(
        "All Ratings" to "all",
        "Safe" to "safe",
        "Questionable" to "questionable",
        "Explicit" to "explicit"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar Area
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        var isSearchFocused by remember { mutableStateOf(false) }
        val recentSearches by viewModel.recentSearches.collectAsState()
        val suggestions by viewModel.autocompleteSuggestions.collectAsState()

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onFocusChanged { isSearchFocused = it.isFocused },
            placeholder = { Text("Search tags (e.g. effects yutaka_nakamura)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = { 
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        viewModel.updateSearchQuery("")
                        viewModel.search("") 
                        keyboardController?.hide()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } 
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { 
                viewModel.search(searchQuery.trim())
                isSearchFocused = false
                keyboardController?.hide()
            })
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sakuga Extended Search",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { expandedFilters = !expandedFilters },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (expandedFilters) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Filters",
                            tint = if (expandedFilters) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (expandedFilters) {
                    val sortOrder by viewModel.sortOrder.collectAsState()
                    val ratingFilter by viewModel.ratingFilter.collectAsState()
                    val postsLimit by viewModel.postsLimit.collectAsState()
                    val isSoloKa by viewModel.isSoloKa.collectAsState()

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FilterDropdown(
                                        label = "Sort by",
                                        selectedValue = sortOrder,
                                        options = sortOptions,
                                        onValueChange = { viewModel.updateSortOrder(it) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FilterDropdown(
                                        label = "Rating",
                                        selectedValue = ratingFilter,
                                        options = ratingOptions,
                                        onValueChange = { viewModel.updateRatingFilter(it) }
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = postsLimit,
                                    onValueChange = { viewModel.updatePostsLimit(it) },
                                    label = { Text("Posts Limit", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1.1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            viewModel.commitPostsLimitSearch()
                                            keyboardController?.hide()
                                        }
                                    ),
                                    trailingIcon = {
                                        IconButton(
                                            modifier = Modifier.size(36.dp),
                                            onClick = { 
                                                viewModel.commitPostsLimitSearch()
                                                keyboardController?.hide()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward, 
                                                contentDescription = "Apply limit",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = TextStyle(fontSize = 13.sp)
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable { viewModel.toggleSoloKa(!isSoloKa) }
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = isSoloKa,
                                        onCheckedChange = { viewModel.toggleSoloKa(it) }
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "Solo KA", 
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text(
                                text = "Booru Cheat Sheet Shortcuts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val helperChips = listOf(
                                    "Score >= 10" to "score:>=10",
                                    "Exclude Sibling Clips" to "parent:none",
                                    "Only HD (1080p)" to "width:>=1920",
                                    "Web Animation" to "web_animation",
                                    "Remove Sound" to "-sound",
                                    "Yutapon Cubes" to "yutapon_cubes"
                                )
                                helperChips.forEach { (label, searchSuffix) ->
                                    AssistChip(
                                        onClick = {
                                            val currentQuery = searchQuery.trim()
                                            val newQuery = if (currentQuery.contains(searchSuffix)) {
                                                currentQuery 
                                            } else if (currentQuery.isEmpty()) {
                                                searchSuffix
                                            } else {
                                                "$currentQuery $searchSuffix"
                                            }
                                            viewModel.updateSearchQuery(newQuery)
                                            viewModel.search(newQuery)
                                        },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tags = listOf(
                        Triple("Yutaka Nakamura", "yutaka_nakamura", MaterialTheme.colorScheme.surfaceVariant),
                        Triple("Effects", "effects", MaterialTheme.colorScheme.primary),
                        Triple("Action", "action", MaterialTheme.colorScheme.surfaceVariant),
                        Triple("Smear", "smear", MaterialTheme.colorScheme.surfaceVariant),
                        Triple("Background Animation", "background_animation", MaterialTheme.colorScheme.surfaceVariant),
                        Triple("Character Acting", "character_acting", MaterialTheme.colorScheme.surfaceVariant)
                    )
                    tags.forEach { (displayName, tagValue, bgColor) ->
                        val textColor = if (bgColor == MaterialTheme.colorScheme.primary) 
                            MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable { 
                                    val newQuery = if (searchQuery.isEmpty()) tagValue else "$searchQuery $tagValue"
                                    viewModel.updateSearchQuery(newQuery)
                                    viewModel.search(newQuery) 
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = displayName,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isLoading && posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(posts) { post ->
                            PostItem(post = post, viewModel = viewModel, onClick = { onPostClick(post) })
                        }
                        if (posts.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LaunchedEffect(posts.size) {
                                    viewModel.loadMore()
                                }
                                if (isLoading) {
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

            if (isSearchFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { isSearchFocused = false }
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 300.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (searchQuery.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (recentSearches.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                        Text(
                                            text = "Clear All",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (recentSearches.isEmpty()) {
                                Text(
                                    text = "No recent searches yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    recentSearches.forEach { term ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.background)
                                                .clickable {
                                                    viewModel.updateSearchQuery(term)
                                                    viewModel.search(term)
                                                    isSearchFocused = false
                                                    keyboardController?.hide()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = term,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Tag Predictions",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (suggestions.isEmpty()) {
                                Text(
                                    text = "No suggestions found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    suggestions.forEach { tag ->
                                        val cat = com.example.data.SakugaTagCategory.fromId(tag.type)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateSearchQuery(tag.name)
                                                    viewModel.search(tag.name)
                                                    isSearchFocused = false
                                                    keyboardController?.hide()
                                                }
                                                .padding(vertical = 8.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = tag.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                color = Color(cat.darkColorHex),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${tag.count} • ${cat.displayName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
fun PostItem(post: SakugaPost, viewModel: SakugaViewModel, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            ) {
                AsyncImage(
                    model = post.previewUrl,
                    contentDescription = "Post preview",
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
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Render beautiful categorised colorful tag chips
                val tagsList = remember(post.tags) {
                    post.tags.split(" ").filter { it.isNotEmpty() }.take(3)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (tagsList.isEmpty()) {
                        Text(
                            text = "UNKNOWN",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    } else {
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
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Score: ${post.score}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val extSizeText = if (post.fileExt != null) "${post.fileExt.uppercase()} • ID: ${post.id}" else "ID: ${post.id}"
                Text(
                    text = extSizeText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayName = options.find { it.second == selectedValue }?.first ?: selectedValue
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { (displayName, codeValue) ->
                DropdownMenuItem(
                    text = { Text(displayName, fontSize = 14.sp) },
                    onClick = {
                        onValueChange(codeValue)
                        expanded = false
                    }
                )
            }
        }
    }
}
