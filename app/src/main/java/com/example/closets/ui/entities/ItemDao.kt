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

    @Query("SELECT * FROM items")
    suspend fun getAllItemsDirectly(): List<Item>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    fun getItemById(itemId: Int): LiveData<Item>

    @Query("SELECT * FROM items WHERE id IN (:itemIds)")
    suspend fun getItemsByIds(itemIds: List<Int>): List<Item>

    @Query("SELECT * FROM items WHERE isFavorite = 1")
    fun getFavoriteItems(): LiveData<List<Item>>

    @Query("SELECT * FROM items WHERE lastWornDate < date('now', '-3 months')")
    fun getUnusedItems(): LiveData<List<Item>>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemCount(): Int

    @Query("UPDATE items SET isFavorite = :isFavorite WHERE id = :itemId")
    suspend fun updateItemFavoriteStatus(itemId: Int, isFavorite: Boolean)

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirectly(id: Int): Item?

    @Query("DELETE FROM items WHERE id IN (:itemIds)")
    suspend fun deleteItemsByIds(itemIds: List<Int>)

    @Query("DELETE FROM items")
    suspend fun clearAllItems()

    @Update
    suspend fun updateItem(item: Item)

    @Update
    suspend fun updateItems(items: List<Item>)

    @Delete
    suspend fun deleteItem(item: Item)
}