package com.example.data

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class SakugaRepository(context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "sakuga_database"
    ).build()

    private val postDao = database.postDao()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.sakugabooru.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(SakugaApi::class.java)

    val savedPosts: Flow<List<SakugaPost>> = postDao.getAllPosts()

    private val tagCache = java.util.concurrent.ConcurrentHashMap<String, SakugaTag>()

    suspend fun getTagInfo(tagName: String): SakugaTag? {
        val sanitized = tagName.lowercase().trim()
        val cached = tagCache[sanitized]
        if (cached != null) return cached

        return try {
            val response = api.getTags(name = sanitized, limit = 1)
            val tag = response.firstOrNull()
            if (tag != null) {
                tagCache[sanitized] = tag
                tag
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchPopularTags(limit: Int = 300) {
        try {
            val response = api.getTags(limit = limit, order = "count")
            response.forEach { tag ->
                tagCache[tag.name.lowercase().trim()] = tag
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAutocompleteTags(query: String): List<SakugaTag> {
        val sanitized = query.lowercase().trim()
        if (sanitized.isEmpty()) return emptyList()

        // Seek local matches from tag cache
        val localMatches = tagCache.values
            .filter { it.name.lowercase().contains(sanitized) }
            .sortedWith(compareBy<SakugaTag> { !it.name.lowercase().startsWith(sanitized) }.thenBy { it.name.length })
            .take(15)

        if (localMatches.size >= 12) return localMatches

        // If local matches are not enough, query from api with a wildcard query
        return try {
            val apiMatches = api.getTags(name = "*$sanitized*", limit = 20)
            apiMatches.forEach { tag ->
                tagCache[tag.name.lowercase().trim()] = tag
            }
            (localMatches + apiMatches)
                .distinctBy { it.id }
                .sortedWith(compareBy<SakugaTag> { !it.name.lowercase().startsWith(sanitized) }.thenBy { it.name.length })
                .take(15)
        } catch (e: Exception) {
            e.printStackTrace()
            localMatches
        }
    }

    fun isPostSaved(id: Int): Flow<Boolean> = postDao.isPostSaved(id)

    suspend fun savePost(post: SakugaPost) {
        postDao.insertPost(post.copy(savedTimestamp = System.currentTimeMillis()))
    }

    suspend fun removePost(id: Int) {
        postDao.deletePostById(id)
    }

    suspend fun getComments(postId: Int): List<SakugaComment> {
        return try {
            api.getComments(postId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchPosts(tags: String, page: Int = 1, limit: Int = 20): List<SakugaPost> {
        return try {
            api.getPosts(tags = tags.ifEmpty { null }, page = page, limit = limit)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
