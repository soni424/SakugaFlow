package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SakugaComment(
    val id: Int = 0,
    @Json(name = "post_id") val postId: Int = 0,
    val creator: String? = null,
    @Json(name = "creator_id") val creatorId: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    val body: String = ""
)
