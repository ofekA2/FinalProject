package com.example.finalproject.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhoto: String,
    val restaurant: String,
    val city: String,
    val cuisine: String,
    val rating: Double,
    val priceTier: Int,
    val reviewText: String,
    val imageUrl: String,
    val timestampMs: Long
)