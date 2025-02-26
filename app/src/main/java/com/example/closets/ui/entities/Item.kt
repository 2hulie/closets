package com.example.closets.ui.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // primary key with auto-increment
    val name: String,
    val type: String,
    val color: String,
    var wornTimes: Int,
    val imageUri: String?,
    var lastWornDate: String,
    var isFavorite: Boolean,
    var isChecked: Boolean = false,
)