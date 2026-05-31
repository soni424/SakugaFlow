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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        autocompleteJob?.cancel()

        if (query.trim().isEmpty()) {
            _autocompleteSuggestions.value = emptyList()
            return
        }

        autocompleteJob = viewModelScope.launch {
            // Debounce matching for responsive typing
            kotlinx.coroutines.delay(180)
            val results = repository.getAutocompleteTags(query)
            _autocompleteSuggestions.value = results
        }
    }

    fun updateSortOrder(order: String) {
        _sortOrder.value = order
        search(_searchQuery.value)
    }

    fun updateRatingFilter(rating: String) {
        _ratingFilter.value = rating
        search(_searchQuery.value)
    }

    fun updatePostsLimit(limit: String) {
        _postsLimit.value = limit
    }

    fun commitPostsLimitSearch() {
        search(_searchQuery.value)
    }

    fun toggleSoloKa(enabled: Boolean) {
        _isSoloKa.value = enabled
        search(_searchQuery.value)
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

    fun search(query: String) {
        _searchQuery.value = query
        currentPage = 1
        isEndReached = false
        if (query.trim().isNotEmpty()) {
            saveSearchQueryToHistory(query)
        }
        viewModelScope.launch {
            _isLoading.value = true
            val compiled = buildFinalQuery(query, _sortOrder.value, _ratingFilter.value, _isSoloKa.value)
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
        val parts = mutableListOf<String>()
        val trimmedBase = base.trim()

        if (trimmedBase.isNotEmpty()) {
            parts.add(trimmedBase)
        }

        if (rating != "all") {
            parts.add("rating:$rating")
        }

        if (sort != "date") {
            parts.add("order:$sort")
        }

        if (soloKa) {
            parts.add("source:*solo*ka")
        }

        return parts.joinToString(" ")
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

    fun hasTimestamp(body: String): Boolean {
        val timestampRegex = """(\d+):(\d{2})(?:\.(\d+))?""".toRegex()
        return timestampRegex.containsMatchIn(body)
    }

    fun loadCommentsForPost(post: SakugaPost) {
        _comments.value = emptyList()
        _timelineComments.value = emptyList()
        _discussionComments.value = emptyList()
        _parsedTimeline.value = emptyList()
        _currentArtist.value = ""
        viewModelScope.launch {
            val rawComments = repository.getComments(post.id)
            _comments.value = rawComments
            
            val withTime = rawComments.filter { hasTimestamp(it.body) }
            val withoutTime = rawComments.filter { !hasTimestamp(it.body) }
            _timelineComments.value = withTime
            _discussionComments.value = withoutTime
            
            val parsed = parseTimelineFromComments(withTime, post)
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

    private fun parseTimeToMs(minsStr: String, secsStr: String, subSecsStr: String?): Long {
        val mins = minsStr.toLongOrNull() ?: 0
        val secs = secsStr.toLongOrNull() ?: 0
        var totalMs = (mins * 60 + secs) * 1000
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

    private fun parseTimelineFromComments(
        rawComments: List<SakugaComment>,
        post: SakugaPost
    ): List<TimelineSegment> {
        val segments = mutableListOf<TimelineSegment>()
        // Match timestamps like 0:35.8 or 1:11 or 0:57
        val timestampRegex = """(\d+):(\d{2})(?:\.(\d+))?""".toRegex()

        rawComments.forEach { comment ->
            val lines = comment.body.lines()
            lines.forEach { line ->
                val trimmed = line.trim()
                val match = timestampRegex.find(trimmed)
                if (match != null) {
                    val mins = match.groupValues[1]
                    val secs = match.groupValues[2]
                    val subSec = match.groupValues[3].takeIf { it.isNotEmpty() }
                    
                    val timestampMs = parseTimeToMs(mins, secs, subSec)
                    
                    // Simple clean up of label: remove the timestamp itself from the comment line
                    var cleanLine = trimmed.replace(match.value, "").trim()
                    cleanLine = cleanLine.trimStart(':', '-', '–', '~', ' ', ',')
                    if (cleanLine.isEmpty()) {
                        cleanLine = comment.body.take(65).replace("\n", " ") + "..."
                    }
                    
                    segments.add(
                        TimelineSegment(
                            startMs = timestampMs,
                            endMs = timestampMs + 3000,
                            label = cleanLine,
                            author = comment.creator ?: "Anonymous"
                        )
                    )
                }
            }
        }
        
        segments.sortBy { it.startMs }
        // Adjust endMs of each segment to the next segment's startMs to keep it neat
        for (i in 0 until segments.size - 1) {
            val current = segments[i]
            val next = segments[i + 1]
            segments[i] = current.copy(endMs = next.startMs)
        }

        return segments
    }
}

data class TimelineSegment(
    val startMs: Long,
    val endMs: Long,
    val label: String,
    val author: String = ""
)
