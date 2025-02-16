package com.example.closets.ui.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Primary key with auto-increment
    val name: String, //  Name of the item
    val type: String, // Type of the item (e.g., shirt, pants)
    val color: String, // Color of the item
    val wornTimes: Int, // Number of times the item has been worn
    val imageUri: String?,
    val lastWornDate: String = "", // Date when the item was last worn
    var isFavorite: Boolean
)