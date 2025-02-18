package com.example.closets.repository

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.closets.ui.entities.Item
import com.example.closets.ui.entities.ItemDao

@Database(entities = [Item::class], version = 4) // Set the version to 4
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "closets_database"
                )
                    .fallbackToDestructiveMigration() // This will clear the database if migrations fail
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}