package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "saved_posts")
@JsonClass(generateAdapter = true)
data class SakugaPost(
    @PrimaryKey
    val id: Int = 0,
    val tags: String = "",
    @Json(name = "file_url") val fileUrl: String = "",
    @Json(name = "preview_url") val previewUrl: String = "",
    @Json(name = "sample_url") val sampleUrl: String? = null,
    @Json(name = "file_ext") val fileExt: String? = null,
    val score: Int = 0,
    val author: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val savedTimestamp: Long = 0L
)
