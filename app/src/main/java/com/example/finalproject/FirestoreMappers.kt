package com.example.finalproject

import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toSafeReview(): Review {
    fun asString(key: String) = getString(key) ?: ""
    fun asDouble(key: String): Double = when (val v = get(key)) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    fun asInt(key: String): Int = when (val v = get(key)) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: 1
        else -> 1
    }
    val ts = getTimestamp("timestamp")

    return Review(
        id = id,
        restaurant = asString("restaurant"),
        city = asString("city"),
        cuisine = asString("cuisine"),
        priceTier = asInt("priceTier"),
        rating = asDouble("rating"),
        reviewText = asString("reviewText"),
        imageUrl = asString("imageUrl"),
        authorId = asString("authorId"),
        authorName = asString("authorName"),
        authorPhoto = asString("authorPhoto"),
        timestamp = ts
    )
}