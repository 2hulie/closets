package com.example.closets.repository

import androidx.lifecycle.LiveData
import com.example.closets.ui.entities.Item
import com.example.closets.ui.entities.ItemDao

class ItemRepository(private val itemDao: ItemDao) {
    // Insert an item into the database
    suspend fun insertItem(item: Item) {
        itemDao.insertItem(item)
    }

    // Get all items as LiveData for observing changes
    fun getAllItems(): LiveData<List<Item>> {
        return itemDao.getAllItems()
    }

    // Method to get a single item by ID
    fun getItemById(itemId: Int): LiveData<Item> {
        return itemDao.getItemById(itemId)
    }

    fun getFavoriteItems(): LiveData<List<Item>> {
        return itemDao.getFavoriteItems()
    }

    suspend fun updateItemFavoriteStatus(itemId: Int, isFavorite: Boolean) {
        itemDao.updateItemFavoriteStatus(itemId, isFavorite)
    }

    // Update an existing item
    suspend fun updateItem(item: Item) {
        itemDao.updateItem(item)
    }

    // Delete an item from the database
    suspend fun deleteItem(item: Item) {
        itemDao.deleteItem(item)
    }

    // Delete multiple items by their IDs
    suspend fun deleteItemsByIds(itemIds: List<Int>) {
        itemDao.deleteItemsByIds(itemIds) // Implement this in your DAO
    }
}