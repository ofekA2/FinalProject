package com.example.finalproject

import android.app.Application
import androidx.room.Room
import com.example.finalproject.data.AppDatabase

class MyApp : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "my-db"
        ).build()
    }
}