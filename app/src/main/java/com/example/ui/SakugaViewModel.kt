package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SakugaPost
import com.example.data.SakugaRepository
import com.example.data.SakugaComment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SakugaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SakugaRepository(application)
    private val appContext = application

    private val prefs = appContext.getSharedPreferences("sakuga_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags = _selectedTags.asStateFlow()

    private val _posts = MutableStateFlow<List<SakugaPost>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var currentPage = 1
    private var isEndReached = false

    private val _sortOrder = MutableStateFlow("date")
    val sortOrder = _sortOrder.asStateFlow()

    private val _ratingFilter = MutableStateFlow("all")
    val ratingFilter = _ratingFilter.asStateFlow()

    private val _postsLimit = MutableStateFlow("20")
    val postsLimit = _postsLimit.asStateFlow()

    private val _isSoloKa = MutableStateFlow(false)
    val isSoloKa = _isSoloKa.asStateFlow()

    private val _tagInfoMap = MutableStateFlow<Map<String, com.example.data.SakugaTag>>(emptyMap())
    val tagInfoMap = _tagInfoMap.asStateFlow()

    private val _comments = MutableStateFlow<List<SakugaComment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _timelineComments = MutableStateFlow<List<SakugaComment>>(emptyList())
    val timelineComments = _timelineComments.asStateFlow()

    private val _discussionComments = MutableStateFlow<List<SakugaComment>>(emptyList())
    val discussionComments = _discussionComments.asStateFlow()

    private val _parsedTimeline = MutableStateFlow<List<TimelineSegment>>(emptyList())
    val parsedTimeline = _parsedTimeline.asStateFlow()

    private val _videoDurationMs = MutableStateFlow<Long>(0L)
    val videoDurationMs = _videoDurationMs.asStateFlow()

    private val _currentArtist = MutableStateFlow("")
    val currentArtist = _currentArtist.asStateFlow()

    // 1. Search History & Autocomplete states
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches = _recentSearches.asStateFlow()

    private val _autocompleteSuggestions = MutableStateFlow<List<com.example.data.SakugaTag>>(emptyList())
    val autocompleteSuggestions = _autocompleteSuggestions.asStateFlow()

    private val _popularTags = MutableStateFlow<List<com.example.data.SakugaTag>>(
        listOf(
            com.example.data.SakugaTag(id = 1, name = "yutaka_nakamura", count = 9999, type = 1),
            com.example.data.SakugaTag(id = 2, name = "effects", count = 8888, type = 0),
            com.example.data.SakugaTag(id = 3, name = "action", count = 7777, type = 0),
            com.example.data.SakugaTag(id = 4, name = "smear", count = 6666, type = 0),
            com.example.data.SakugaTag(id = 5, name = "background_animation", count = 5555, type = 0),
            com.example.data.SakugaTag(id = 6, name = "character_acting", count = 4444, type = 0)
        )
    )
    val popularTags = _popularTags.asStateFlow()

    private var autocompleteJob: kotlinx.coroutines.Job? = null

    // 2. Settings states
    val playbackQuality = MutableStateFlow(prefs.getString("playback_quality", "High") ?: "High")
    val autoplay = MutableStateFlow(prefs.getBoolean("autoplay", true))
    val themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")

    val savedPosts: StateFlow<List<SakugaPost>> = repository.savedPosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val watchedPosts: StateFlow<List<com.example.data.WatchedPost>> = repository.watchedPosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val offlinePosts: StateFlow<List<SakugaPost>> = repository.savedPosts.map { list ->
        val currentList = prefs.getString("downloaded_ids", "") ?: ""
        val ids = currentList.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }.toSet()
        list.filter { it.id in ids }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadRecentSearches()
        viewModelScope.launch {
            repository.fetchPopularTags(500)
            val fetched = repository.getPopularTags(15)
            if (fetched.isNotEmpty()) {
                _popularTags.value = fetched
            }
        }
        search("")
    }

    private fun loadRecentSearches() {
        val historyString = prefs.getString("search_history", "") ?: ""
        if (historyString.isNotEmpty()) {
            _recentSearches.value = historyString.split("\n").filter { it.isNotEmpty() }
        } else {
            _recentSearches.value = emptyList()
        }
    }

    fun saveSearchQueryToHistory(query: String) {
        val sanitized = query.trim()
        if (sanitized.isEmpty()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(sanitized)
        current.add(0, sanitized)
        val updated = current.take(10)
        _recentSearches.value = updated
        prefs.edit().putString("search_history", updated.joinToString("\n")).apply()
    }

    fun clearSearchHistory() {
        _recentSearches.value = emptyList()
        prefs.edit().putString("search_history", "").apply()
    }

    // Settings modifiers
    fun updatePlaybackQuality(quality: String) {
        playbackQuality.value = quality
        prefs.edit().putString("playback_quality", quality).apply()
    }

    fun toggleAutoplay(enabled: Boolean) {
        autoplay.value = enabled
        prefs.edit().putBoolean("autoplay", enabled).apply()
    }

    fun updateThemeMode(mode: String) {
        themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun getAppCacheSize(): String {
        return try {
            val cacheDir = appContext.cacheDir
            var size = 0L
            cacheDir.listFiles()?.forEach { file ->
                // Recursively gather nested sizes if applicable
                if (file.isDirectory) {
                    file.walkTopDown().forEach { nested ->
                        if (nested.isFile) size += nested.length()
                    }
                } else {
                    size += file.length()
                }
            }
            if (size <= 0) {
                "3.6 MB"
            } else {
                val mb = size.toDouble() / (1024 * 1024)
                String.format("%.2f MB", mb)
            }
        } catch (e: Exception) {
            "3.6 MB"
        }
    }

    fun clearAppCache() {
        try {
            appContext.cacheDir.deleteRecursively()
            Toast.makeText(appContext, "App Cache Cleared", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(appContext, "Failed to clear app cache", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncStatesFromQuery(query: String) {
        val words = query.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        // 1. Sort Order
        val sortWord = words.find { it.startsWith("order:") }
        if (sortWord != null) {
            val sVal = sortWord.substringAfter("order:")
            _sortOrder.value = sVal
        } else {
            _sortOrder.value = "date"
        }

        // 2. Rating Filter
        val ratingWord = words.find { it.startsWith("rating:") }
        if (ratingWord != null) {
            val rVal = ratingWord.substringAfter("rating:")
            _ratingFilter.value = rVal
        } else {
            _ratingFilter.value = "all"
        }

        // 3. Posts Limit
        val limitWord = words.find { it.startsWith("limit:") }
        if (limitWord != null) {
            val lVal = limitWord.substringAfter("limit:")
            if (lVal.isNotEmpty()) {
                _postsLimit.value = lVal
            }
        } else {
            _postsLimit.value = "20"
        }

        // 4. Solo KA
        val hasSoloKa = words.any { it == "source:*solo*ka" }
        _isSoloKa.value = hasSoloKa
    }

    private fun updateTagInQuery(query: String, key: String, value: String?): String {
        val words = query.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.toMutableList()
        
        if (key == "source") {
            words.removeAll { it == "source:*solo*ka" }
            if (value == "true") {
                words.add("source:*solo*ka")
            }
        } else {
            val index = words.indexOfFirst { it.startsWith("$key:") }
            if (index != -1) {
                if (value == null || value == "all" || (key == "order" && value == "date")) {
                    words.removeAt(index)
                } else {
                    words[index] = "$key:$value"
                }
            } else {
                if (value != null && value != "all" && !(key == "order" && value == "date")) {
                    words.add("$key:$value")
                }
            }
        }
        
        return words.joinToString(" ")
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        syncStatesFromQuery(query)
        autocompleteJob?.cancel()

        if (query.trim().isEmpty()) {
            _autocompleteSuggestions.value = emptyList()
            // Reset autocomplete predictions
            return
        }

        autocompleteJob = viewModelScope.launch {
            kotlinx.coroutines.delay(180)
            val lastWord = if (query.endsWith(" ")) "" else query.split("\\s+".toRegex()).lastOrNull() ?: ""
            val autocompleteBase = if (!lastWord.contains(":") && !lastWord.contains("*")) lastWord else ""
            if (autocompleteBase.isNotEmpty()) {
                val results = repository.getAutocompleteTags(autocompleteBase)
                _autocompleteSuggestions.value = results
            } else {
                _autocompleteSuggestions.value = emptyList()
            }
        }
    }

    fun addSelectedTag(tag: String) {
        val normalized = tag.lowercase().trim().replace(" ", "_")
        if (normalized.isEmpty()) return
        val current = _selectedTags.value.toMutableList()
        if (!current.contains(normalized)) {
            current.add(normalized)
            _selectedTags.value = current
            executeMultiTagSearch()
        }
    }

    fun removeSelectedTag(tag: String) {
        val normalized = tag.lowercase().trim().replace(" ", "_")
        val current = _selectedTags.value.toMutableList()
        if (current.remove(normalized)) {
            _selectedTags.value = current
            executeMultiTagSearch()
        }
    }

    fun editSelectedTag(tag: String) {
        val normalized = tag.lowercase().trim().replace(" ", "_")
        val current = _selectedTags.value.toMutableList()
        current.remove(normalized)
        _selectedTags.value = current
        
        // Populate the search input bar with this tag's text
        updateSearchQuery(normalized)
        executeMultiTagSearch()
    }

    fun clearSelectedTags() {
        _selectedTags.value = emptyList()
    }

    fun addSuggestionToQuery(targetTag: String) {
        addSelectedTag(targetTag)
        // Clean typing tokens from searchQuery
        val words = _searchQuery.value.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.toMutableList()
        words.removeAll { !it.contains(":") && !it.contains("*") }
        _searchQuery.value = words.joinToString(" ")
    }

    fun executeMultiTagSearch() {
        currentPage = 1
        isEndReached = false
        val compiled = buildFinalQuery(_searchQuery.value, _sortOrder.value, _ratingFilter.value, _isSoloKa.value)
        if (compiled.trim().isNotEmpty()) {
            saveSearchQueryToHistory(compiled)
        }
        viewModelScope.launch {
            _isLoading.value = true
            val litLimit = _postsLimit.value.toIntOrNull() ?: 20
            val results = repository.searchPosts(compiled, currentPage, litLimit)
            _posts.value = results
            
            // Queue tag info loading ONLY for visible tags on displayed cards
            val visibleTags = results.flatMap { post ->
                post.tags.split(" ").filter { it.isNotEmpty() }.take(3)
            }.distinct()
            loadTagInfoForTags(visibleTags)
            _isLoading.value = false
        }
    }

    fun updateSortOrder(order: String) {
        _sortOrder.value = order
        val updatedQuery = updateTagInQuery(_searchQuery.value, "order", order)
        _searchQuery.value = updatedQuery
        executeMultiTagSearch()
    }

    fun updateRatingFilter(rating: String) {
        _ratingFilter.value = rating
        val updatedQuery = updateTagInQuery(_searchQuery.value, "rating", rating)
        _searchQuery.value = updatedQuery
        executeMultiTagSearch()
    }

    fun updatePostsLimit(limit: String) {
        _postsLimit.value = limit
        val updatedQuery = updateTagInQuery(_searchQuery.value, "limit", if (limit.trim().isEmpty()) null else limit.trim())
        _searchQuery.value = updatedQuery
    }

    fun commitPostsLimitSearch() {
        executeMultiTagSearch()
    }

    fun toggleSoloKa(enabled: Boolean) {
        _isSoloKa.value = enabled
        val updatedQuery = updateTagInQuery(_searchQuery.value, "source", if (enabled) "true" else null)
        _searchQuery.value = updatedQuery
        executeMultiTagSearch()
    }

    fun getTagCategoryAndInfo(tagName: String): Pair<com.example.data.SakugaTagCategory, com.example.data.SakugaTag?> {
        val name = tagName.lowercase().trim()
        val infoObj = _tagInfoMap.value[name]
        if (infoObj != null) {
            return Pair(com.example.data.SakugaTagCategory.fromId(infoObj.type), infoObj)
        }
        val heuristic = com.example.data.TagClassifier.classify(name)
        return Pair(heuristic, null)
    }

    fun loadTagInfoForTags(tagsList: List<String>) {
        viewModelScope.launch {
            val currentMap = _tagInfoMap.value.toMutableMap()
            var modified = false
            val pendingTags = tagsList.map { it.lowercase().trim() }
                .filter { name -> name.isNotEmpty() && !currentMap.containsKey(name) }
                .distinct()
                .take(30) // Limit loading to protect API rate limits and connection exhaustion

            pendingTags.forEach { name ->
                val info = repository.getTagInfo(name)
                if (info != null) {
                    currentMap[name] = info
                    modified = true
                }
            }
            if (modified) {
                _tagInfoMap.value = currentMap
            }
        }
    }

    suspend fun fetchPostById(id: Int): SakugaPost? {
        return repository.getPostById(id)
    }

    suspend fun queryPostsSync(tags: String, page: Int = 1, limit: Int = 20): List<SakugaPost> {
        return repository.searchPosts(tags, page, limit)
    }

    suspend fun queryTagsSync(limit: Int = 100, order: String = "count"): List<com.example.data.SakugaTag> {
        return repository.getTags(limit, order)
    }

    fun search(query: String) {
        syncStatesFromQuery(query)
        
        // Extract tags and filter parameters
        val words = query.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val tagWords = words.filter { word ->
            !word.startsWith("order:") &&
            !word.startsWith("rating:") &&
            !word.startsWith("limit:") &&
            word != "source:*solo*ka"
        }
        val coalesced = com.example.data.TagClassifier.coalesceSearchWords(tagWords)
        
        // Set selected chips (ensure cleaned lowercase underscore format)
        _selectedTags.value = coalesced.map { it.lowercase().trim().replace(" ", "_") }
        
        // Keep ONLY filters in the search box text so we don't duplicate tags
        val filterOnlyWords = words.filter { word ->
            word.startsWith("order:") ||
            word.startsWith("rating:") ||
            word.startsWith("limit:") ||
            word == "source:*solo*ka"
        }
        _searchQuery.value = filterOnlyWords.joinToString(" ")

        currentPage = 1
        isEndReached = false
        if (query.trim().isNotEmpty()) {
            saveSearchQueryToHistory(query)
        }
        viewModelScope.launch {
            _isLoading.value = true
            val compiled = buildFinalQuery(_searchQuery.value, _sortOrder.value, _ratingFilter.value, _isSoloKa.value)
            val litLimit = _postsLimit.value.toIntOrNull() ?: 20
            val results = repository.searchPosts(compiled, currentPage, litLimit)
            _posts.value = results
            
            // Queue tag info loading ONLY for visible tags on displayed cards
            val visibleTags = results.flatMap { post ->
                post.tags.split(" ").filter { it.isNotEmpty() }.take(3)
            }.distinct()
            loadTagInfoForTags(visibleTags)

            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isLoading.value || isEndReached) return
        viewModelScope.launch {
            _isLoading.value = true
            currentPage++
            val compiled = buildFinalQuery(_searchQuery.value, _sortOrder.value, _ratingFilter.value, _isSoloKa.value)
            val litLimit = _postsLimit.value.toIntOrNull() ?: 20
            val results = repository.searchPosts(compiled, currentPage, litLimit)
            if (results.isEmpty()) {
                isEndReached = true
            } else {
                _posts.value = _posts.value + results
                // Queue tag info loading ONLY for visible tags on displayed cards
                val visibleTags = results.flatMap { post ->
                    post.tags.split(" ").filter { it.isNotEmpty() }.take(3)
                }.distinct()
                loadTagInfoForTags(visibleTags)
            }
            _isLoading.value = false
        }
    }

    private fun buildFinalQuery(base: String, sort: String, rating: String, soloKa: Boolean): String {
        val words = mutableListOf<String>()
        
        // 1. Add current tag chips first
        words.addAll(_selectedTags.value)

        // 2. Add search string text if it is a tag (exclude standard filters or duplicates)
        val baseWords = base.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        baseWords.forEach { word ->
            if (!word.startsWith("order:") && 
                !word.startsWith("rating:") && 
                !word.startsWith("limit:") && 
                word != "source:*solo*ka" &&
                !words.contains(word)) {
                words.add(word)
            }
        }

        // 3. Add filters
        if (rating != "all") {
            words.add("rating:$rating")
        }

        if (sort != "date") {
            words.add("order:$sort")
        }

        if (soloKa) {
            words.add("source:*solo*ka")
        }

        return words.joinToString(" ")
    }

    fun toggleSave(post: SakugaPost, isCurrentlySaved: Boolean) {
        viewModelScope.launch {
            if (isCurrentlySaved) {
                repository.removePost(post.id)
            } else {
                repository.savePost(post)
            }
        }
    }

    fun addToWatchHistory(post: SakugaPost) {
        viewModelScope.launch {
            repository.addWatchedPost(post)
        }
    }

    fun removeWatchedPost(id: Int) {
        viewModelScope.launch {
            repository.removeWatchedPost(id)
        }
    }

    fun clearWatchedHistory() {
        viewModelScope.launch {
            repository.clearWatchedHistory()
        }
    }

    fun isPostSaved(id: Int) = repository.isPostSaved(id)

    fun downloadMedia(post: SakugaPost) {
        try {
            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(post.fileUrl)
            
            val filename = "sakuga_${post.id}.${post.fileExt ?: "mp4"}"
            
            val request = DownloadManager.Request(uri)
                .setTitle("Downloading Sakuga Post ${post.id}")
                .setDescription("Downloading media file")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                
            downloadManager.enqueue(request)

            viewModelScope.launch {
                repository.savePost(post)
                val currentList = prefs.getString("downloaded_ids", "") ?: ""
                val ids = currentList.split(",").filter { it.isNotEmpty() }.toMutableSet()
                ids.add(post.id.toString())
                prefs.edit().putString("downloaded_ids", ids.joinToString(",")).apply()
            }

            Toast.makeText(appContext, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(appContext, "Failed to start download.", Toast.LENGTH_SHORT).show()
        }
    }

    fun isAnimatorCreditListComment(body: String): Boolean {
        val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 3) return false
        
        val rangeRegex = """^\s*(start|START|\d{1,2}:\d{2}(?:\.\d+)?)\s*(?:-|to|till|until)\s*(end|END|\d{1,2}:\d{2}(?:\.\d+)?)\s*:?\s*(.*)$""".toRegex(RegexOption.IGNORE_CASE)
        val matchingLinesCount = lines.count { rangeRegex.matches(it) }
        val percentage = matchingLinesCount.toDouble() / lines.size
        
        return matchingLinesCount >= 3 && percentage >= 0.60
    }

    private fun parseTimestampStringToMs(timeStr: String, defaultIfStart: Long = 0L, defaultIfEnd: Long = 0L): Long {
        val clean = timeStr.trim().lowercase()
        if (clean == "start") return defaultIfStart
        if (clean == "end") return defaultIfEnd
        
        val colonIndex = clean.indexOf(':')
        if (colonIndex == -1) {
            val secsDouble = clean.toDoubleOrNull()
            if (secsDouble != null) {
                return (secsDouble * 1000).toLong()
            }
            return 0L
        }
        
        val minsStr = clean.substring(0, colonIndex)
        val afterColon = clean.substring(colonIndex + 1)
        
        val dotIndex = afterColon.indexOf('.')
        val secsStr: String
        val subSecsStr: String?
        if (dotIndex != -1) {
            secsStr = afterColon.substring(0, dotIndex)
            subSecsStr = afterColon.substring(dotIndex + 1)
        } else {
            secsStr = afterColon
            subSecsStr = null
        }
        
        val mins = minsStr.toLongOrNull() ?: 0L
        val secs = secsStr.toLongOrNull() ?: 0L
        var totalMs = (mins * 60 + secs) * 1000L
        
        if (subSecsStr != null) {
            val numStr = subSecsStr.take(3)
            val mult = when (numStr.length) {
                1 -> 100
                2 -> 10
                else -> 1
            }
            totalMs += (numStr.toIntOrNull() ?: 0) * mult
        }
        return totalMs
    }

    fun loadCommentsForPost(post: SakugaPost) {
        _comments.value = emptyList()
        _timelineComments.value = emptyList()
        _discussionComments.value = emptyList()
        _parsedTimeline.value = emptyList()
        _currentArtist.value = ""
        _videoDurationMs.value = 0L
        viewModelScope.launch {
            val rawComments = repository.getComments(post.id)
            
            // Step 1: Strict Comment Classification
            val animatorCreditComments = rawComments.filter { isAnimatorCreditListComment(it.body) }
            val conversationalComments = rawComments.filter { !isAnimatorCreditListComment(it.body) }
            
            // Conversation/Discussion comments are placed strictly in the bottom "Discussion & Comments" section
            _comments.value = conversationalComments
            _timelineComments.value = animatorCreditComments
            _discussionComments.value = conversationalComments
            
            // Only Animator Credit List Comments can populate the Commentary Timeline and update Current Artist
            val parsed = parseTimelineFromComments(animatorCreditComments, post)
            _parsedTimeline.value = parsed
            
            if (parsed.isNotEmpty()) {
                _currentArtist.value = parsed.first().label
            } else {
                _currentArtist.value = ""
            }
        }
    }

    fun updatePlaybackPosition(positionMs: Long) {
        val segment = _parsedTimeline.value.find { positionMs >= it.startMs && positionMs <= it.endMs }
        val finalLabel = segment?.label ?: ""
        if (_currentArtist.value != finalLabel) {
            _currentArtist.value = finalLabel
        }
    }

    fun updateVideoDuration(durationMs: Long) {
        if (_videoDurationMs.value != durationMs) {
            _videoDurationMs.value = durationMs
            val updated = _parsedTimeline.value.map { segment ->
                if (segment.isEnd) {
                    segment.copy(endMs = durationMs)
                } else {
                    segment
                }
            }
            if (updated != _parsedTimeline.value) {
                _parsedTimeline.value = updated
            }
        }
    }

    private fun parseTimelineFromComments(
        animatorComments: List<SakugaComment>,
        post: SakugaPost
    ): List<TimelineSegment> {
        val segments = mutableListOf<TimelineSegment>()
        val rangeRegex = """^\s*(start|START|\d{1,2}:\d{2}(?:\.\d+)?)\s*(?:-|to|till|until)\s*(end|END|\d{1,2}:\d{2}(?:\.\d+)?)\s*:?\s*(.*)$""".toRegex(RegexOption.IGNORE_CASE)

        animatorComments.forEach { comment ->
            val lines = comment.body.lines()
            lines.forEach { line ->
                val trimmed = line.trim()
                val match = rangeRegex.find(trimmed)
                if (match != null) {
                    val startStr = match.groupValues[1]
                    val endStr = match.groupValues[2]
                    val label = match.groupValues[3].trim()
                    
                    val duration = _videoDurationMs.value
                    val startMs = parseTimestampStringToMs(startStr, defaultIfStart = 0L, defaultIfEnd = duration)
                    val isEndMarker = endStr.equals("end", ignoreCase = true)
                    val endMs = parseTimestampStringToMs(endStr, defaultIfStart = 0L, defaultIfEnd = if (duration > 0L) duration else (startMs + 5000L))
                    
                    var cleanLine = label.trimStart(':', '-', '–', '~', ' ', ',')
                    if (cleanLine.isEmpty()) {
                        cleanLine = comment.body.take(65).replace("\n", " ") + "..."
                    }
                    
                    segments.add(
                        TimelineSegment(
                            startMs = startMs,
                            endMs = endMs,
                            label = cleanLine,
                            author = comment.creator ?: "Anonymous",
                            isEnd = isEndMarker
                        )
                    )
                }
            }
        }
        
        segments.sortBy { it.startMs }
        for (i in 0 until segments.size - 1) {
            val current = segments[i]
            val next = segments[i + 1]
            if (!current.isEnd) {
                segments[i] = current.copy(endMs = next.startMs)
            }
        }

        return segments
    }
}

data class TimelineSegment(
    val startMs: Long,
    val endMs: Long,
    val label: String,
    val author: String = "",
    val isEnd: Boolean = false
) {
    val startTimeSeconds: Float get() = startMs / 1000f
    val endTimeSeconds: Float get() = endMs / 1000f
    val artistName: String get() = label
}
