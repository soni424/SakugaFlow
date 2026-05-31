package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_posts")
data class WatchedPost(
    @PrimaryKey
    val id: Int,
    val tags: String = "",
    val fileUrl: String = "",
    val previewUrl: String = "",
    val sampleUrl: String? = null,
    val fileExt: String? = null,
    val score: Int = 0,
    val author: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val watchedTimestamp: Long = 0L
)
