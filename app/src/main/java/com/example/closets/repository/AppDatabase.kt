package com.example.closets.repository

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.closets.ui.entities.Item
import com.example.closets.ui.entities.ItemDao

@Database(entities = [Item::class], version = 4) // Increment version to 4
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new column 'isFavorite' to the existing 'items' table
                database.execSQL("ALTER TABLE items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0") // Default to 0 (unfavorited)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "closets_database"
                )
                    .addMigrations(MIGRATION_3_4) // Add the migration here
                    // .fallbackToDestructiveMigration() // Uncomment if want to reset the DB on schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}