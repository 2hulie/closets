package com.example.closets.repository

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.closets.ui.entities.Item
import com.example.closets.ui.entities.ItemDao

@Database(entities = [Item::class], version = 5) // incremented version to 5
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // migration from version 4 to version 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // create a new table with the new schema
                db.execSQL("""
                    CREATE TABLE new_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        color TEXT NOT NULL,
                        wornTimes INTEGER NOT NULL,
                        imageUri TEXT,
                        lastWornDate TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        isChecked INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // copy the data from the old table to the new table
                db.execSQL("""
                    INSERT INTO new_items (id, name, type, color, wornTimes, imageUri, lastWornDate, isFavorite)
                    SELECT id, name, type, color, wornTimes, imageUri, lastWornDate, isFavorite FROM items
                """)

                // remove the old table
                db.execSQL("DROP TABLE items")

                // rename the new table to the old table name
                db.execSQL("ALTER TABLE new_items RENAME TO items")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "closets_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}