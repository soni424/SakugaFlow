package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM saved_posts ORDER BY savedTimestamp DESC")
    fun getAllPosts(): Flow<List<SakugaPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SakugaPost)

    @Query("DELETE FROM saved_posts WHERE id = :id")
    suspend fun deletePostById(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_posts WHERE id = :id)")
    fun isPostSaved(id: Int): Flow<Boolean>
}
