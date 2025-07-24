package com.example.finalproject.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Query("SELECT * FROM reviews ORDER BY timestampMs DESC")
    fun getAll(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE authorId = :uid ORDER BY timestampMs DESC")
    fun getByUser(uid: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reviews: List<ReviewEntity>)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteById(id: String): Int
}