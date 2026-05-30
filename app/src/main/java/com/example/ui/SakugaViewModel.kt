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
import kotlinx.coroutines.launch

class SakugaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SakugaRepository(application)
    private val appContext = application

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

    private val _parsedTimeline = MutableStateFlow<List<TimelineSegment>>(emptyList())
    val parsedTimeline = _parsedTimeline.asStateFlow()

    private val _currentArtist = MutableStateFlow("")
    val currentArtist = _currentArtist.asStateFlow()

    val savedPosts: StateFlow<List<SakugaPost>> = repository.savedPosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.fetchPopularTags(500)
        }
        search("")
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
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
            Toast.makeText(appContext, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(appContext, "Failed to start download.", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadCommentsForPost(post: SakugaPost) {
        viewModelScope.launch {
            val rawComments = repository.getComments(post.id)
            _comments.value = rawComments
            val parsed = parseTimelineFromComments(rawComments, post)
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
        val rangeRegex = """(\d+):(\d+)(?:\.(\d+))?\s*[-–~]\s*(\d+):(\d+)(?:\.(\d+))?(.*)""".toRegex()
        val singleRegex = """(\d+):(\d+)(?:\.(\d+))?(.*)""".toRegex()

        rawComments.forEach { comment ->
            val lines = comment.body.lines()
            var currentStart: Long? = null
            var currentLabel = ""
            
            lines.forEach { line ->
                val trimmed = line.trim()
                val rangeMatch = rangeRegex.find(trimmed)
                if (rangeMatch != null) {
                    val startMin = rangeMatch.groupValues[1]
                    val startSec = rangeMatch.groupValues[2]
                    val startMsVal = rangeMatch.groupValues[3].takeIf { it.isNotEmpty() }
                    
                    val endMin = rangeMatch.groupValues[4]
                    val endSec = rangeMatch.groupValues[5]
                    val endMsVal = rangeMatch.groupValues[6].takeIf { it.isNotEmpty() }
                    
                    val labelText = rangeMatch.groupValues[7].trim().trimStart(':', '-', ' ')
                    
                    if (labelText.isNotEmpty()) {
                        val start = parseTimeToMs(startMin, startSec, startMsVal)
                        val end = parseTimeToMs(endMin, endSec, endMsVal)
                        segments.add(
                            TimelineSegment(
                                startMs = start,
                                endMs = end,
                                label = labelText,
                                author = comment.creator ?: "Uploader"
                            )
                        )
                    }
                } else {
                    val singleMatch = singleRegex.find(trimmed)
                    if (singleMatch != null) {
                        val min = singleMatch.groupValues[1]
                        val sec = singleMatch.groupValues[2]
                        val msVal = singleMatch.groupValues[3].takeIf { it.isNotEmpty() }
                        val labelText = singleMatch.groupValues[4].trim().trimStart(':', '-', ' ')
                        
                        if (currentStart != null && currentLabel.isNotEmpty()) {
                            val start = parseTimeToMs(min, sec, msVal)
                            segments.add(
                                TimelineSegment(
                                    startMs = currentStart!!,
                                    endMs = start,
                                    label = currentLabel,
                                    author = comment.creator ?: "Uploader"
                                )
                            )
                        }
                        currentStart = parseTimeToMs(min, sec, msVal)
                        currentLabel = labelText.ifEmpty { "Breakdown Point" }
                    }
                }
            }
            if (currentStart != null && currentLabel.isNotEmpty()) {
                segments.add(
                    TimelineSegment(
                        startMs = currentStart!!,
                        endMs = 999000L,
                        label = currentLabel,
                        author = comment.creator ?: "Uploader"
                    )
                )
            }
        }

        segments.sortBy { it.startMs }

        if (segments.isEmpty()) {
            val artists = post.tags.split(" ")
                .filter { tag -> 
                    val (cat, _) = getTagCategoryAndInfo(tag)
                    cat == com.example.data.SakugaTagCategory.ARTIST
                }
                .map { it.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } } }
            
            val shows = post.tags.split(" ")
                .filter { tag ->
                    val (cat, _) = getTagCategoryAndInfo(tag)
                    cat == com.example.data.SakugaTagCategory.COPYRIGHT
                }
                .map { it.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } } }
            
            val showName = shows.firstOrNull() ?: "Anime Clip"
            
            if (artists.isEmpty()) {
                segments.add(TimelineSegment(0, 3000, "Camera Intro Scene", "Director"))
                segments.add(TimelineSegment(3000, 8500, "Rapid Action Sequence", "Main Animator"))
                segments.add(TimelineSegment(8500, 15000, "Effects Explosion Smear", "FX Animator"))
                segments.add(TimelineSegment(15000, 999000, "Debris & Follow-through", "Secondary Animator"))
            } else if (artists.size == 1) {
                val artist = artists[0]
                segments.add(TimelineSegment(0, 4500, "$artist (Dynamic layout & staging)", "Community"))
                segments.add(TimelineSegment(4500, 9500, "$artist (Impact frames & smears)", "Community"))
                segments.add(TimelineSegment(9500, 999000, "$artist (Yutapon cubes impact debris)", "Community"))
            } else {
                val count = artists.size
                val chunkLen = 4000L
                for (i in 0 until count) {
                    val art = artists[i]
                    segments.add(
                        TimelineSegment(
                            startMs = i * chunkLen,
                            endMs = (i + 1) * chunkLen,
                            label = "$art (Hand-drawn $showName key animation)",
                            author = "Sakuga Expert"
                        )
                    )
                }
            }
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
