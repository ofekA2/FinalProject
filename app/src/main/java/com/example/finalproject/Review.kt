package com.example.finalproject

import  com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val restaurant: String = "",
    val city: String = "",
    val cuisine: String = "",
    val priceTier: Int = 0,
    val rating: Double = 0.0,
    val reviewText: String = "",
    val imageUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhoto: String = "",
    val timestamp: Timestamp? = null
)