package com.example.closets.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class ItemViewModel(val repository: ItemRepository) : ViewModel() {

    // LiveData for error handling
    val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // LiveData to hold the list of items
    val items: LiveData<List<Item>> = repository.getAllItems()
    val favoriteItems: LiveData<List<Item>> = repository.getFavoriteItems()
    val unusedItems: LiveData<List<Item>> = repository.getUnusedItems()

    fun insert(item: Item) {
        viewModelScope.launch {
            try {
                val count = repository.getItemCount()
                if (count < 50) {
                    repository.insertItem(item)
                } else {
                    _error.value = "You can only have a maximum of 50 items."
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _error.value = "Error inserting item: ${e.message}"
                }
            }
        }
    }

    fun update(item: Item) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _error.value = "Error updating item: ${e.message}"
                }
            }
        }
    }

    fun updateItems(items: List<Item>) {
        viewModelScope.launch {
            try {
                repository.updateItems(items)
            } catch (e: Exception) {
                _error.value = "Error updating items: ${e.message}"
            }
        }
    }

    fun delete(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
            } catch (e: Exception) {
                _error.value = "Error deleting item: ${e.message}"
            }
        }
    }

    fun deleteItems(itemIds: List<Int>) {
        viewModelScope.launch {
            try {
                repository.deleteItemsByIds(itemIds) // Call the repository method to delete items
            } catch (e: Exception) {
                _error.value = "Error deleting items: ${e.message}"
            }
        }
    }

    fun getItem(itemId: Int): LiveData<Item> {
        val result = MutableLiveData<Item>()
        viewModelScope.launch {
            try {
                repository.getItemById(itemId).observeForever { item ->
                    result.value = item
                }
            } catch (e: Exception) {
                _error.value = "Error fetching item: ${e.message}"
            }
        }
        return result
    }

    // Function to clear error
    fun clearError() {
        _error.value = null
    }
}