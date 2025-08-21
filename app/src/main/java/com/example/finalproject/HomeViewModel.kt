package com.example.finalproject

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.ReviewEntity
import com.example.finalproject.data.ReviewDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao: ReviewDao = MyApp.database.reviewDao()
    private val firestore = FirebaseFirestore.getInstance()

    val reviews = dao.getAll().asLiveData()

    init {
        refreshFromRemote()
    }

    private fun refreshFromRemote() {
        viewModelScope.launch(Dispatchers.IO) {
            val docs = firestore.collection("reviews").get().await()

            val list = docs.documents
                .map { it.toSafeReview() }
                .map { r ->
                    ReviewEntity(
                        id = r.id,
                        authorId = r.authorId,
                        authorName = r.authorName,
                        authorPhoto = r.authorPhoto,
                        restaurant = r.restaurant,
                        city = r.city,
                        cuisine = r.cuisine,
                        rating = r.rating,
                        priceTier = r.priceTier,
                        reviewText = r.reviewText,
                        imageUrl = r.imageUrl,
                        timestampMs = r.timestamp?.toDate()?.time ?: 0L
                    )
                }

            dao.upsert(list)
        }
    }
}