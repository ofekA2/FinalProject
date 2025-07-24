package com.example.finalproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.ReviewDao
import com.example.finalproject.data.ReviewEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val dao: ReviewDao = MyApp.database.reviewDao()
    private val firestore = FirebaseFirestore.getInstance()

    val reviews: LiveData<List<ReviewEntity>> = dao.getAll().asLiveData()

    init {
        firestore.collection("reviews")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val entities = snap.documents.mapNotNull { doc ->
                    val data = doc.data?: return@mapNotNull null
                    val ts = (data["timestamp"] as? com.google.firebase.Timestamp) ?: return@mapNotNull null
                    ReviewEntity(
                        id = doc.id,
                        authorId = data["authorId"] as String,
                        authorName = data["authorName"] as String,
                        authorPhoto = data["authorPhoto"] as String,
                        restaurant = data["restaurant"] as String,
                        city = data["city"] as String,
                        cuisine = data["cuisine"] as String,
                        rating = (data["rating"] as Number).toDouble(),
                        priceTier = (data["priceTier"] as Number).toInt(),
                        reviewText = data["reviewText"] as String,
                        imageUrl = data["imageUrl"] as String,
                        timestampMs= ts.toDate().time
                    )
                }
                viewModelScope.launch {
                    dao.upsert(entities)
                }
            }
    }
}