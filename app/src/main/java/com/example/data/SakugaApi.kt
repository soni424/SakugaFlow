package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query

interface SakugaApi {
    @GET("post.json")
    suspend fun getPosts(
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1,
        @Query("tags") tags: String? = null
    ): List<SakugaPost>

    @GET("tag.json")
    suspend fun getTags(
        @Query("name") name: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("order") order: String? = "count"
    ): List<SakugaTag>
}

