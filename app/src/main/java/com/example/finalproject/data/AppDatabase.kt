package com.example.finalproject.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReviewEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}