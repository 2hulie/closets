package com.example.closets.ui.entities

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete


@Dao
interface ItemDao {
    @Insert
    suspend fun insertItem(item: Item)

    @Query("SELECT * FROM items")
    fun getAllItems(): LiveData<List<Item>>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    fun getItemById(itemId: Int): LiveData<Item>

    @Query("SELECT * FROM items WHERE isFavorite = 1")
    fun getFavoriteItems(): LiveData<List<Item>>

    @Query("UPDATE items SET isFavorite = :isFavorite WHERE id = :itemId")
    suspend fun updateItemFavoriteStatus(itemId: Int, isFavorite: Boolean)

    @Query("DELETE FROM items WHERE id IN (:itemIds)")
    suspend fun deleteItemsByIds(itemIds: List<Int>)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)
}