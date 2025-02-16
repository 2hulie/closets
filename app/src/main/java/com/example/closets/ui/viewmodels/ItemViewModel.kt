package com.example.closets.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class ItemViewModel(private val repository: ItemRepository) : ViewModel() {

    // LiveData for error handling
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // LiveData to hold the list of items
    val items: LiveData<List<Item>> = repository.getAllItems()
    val favoriteItems: LiveData<List<Item>> = repository.getFavoriteItems()

    // Function to insert an item
    fun insert(item: Item) {
        viewModelScope.launch {
            try {
                repository.insertItem(item)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _error.value = "Error inserting item: ${e.message}"
                }
            }
        }
    }

    // Function to update an item
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

    // Function to delete a single item
    fun delete(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
            } catch (e: Exception) {
                _error.value = "Error deleting item: ${e.message}"
            }
        }
    }

    // Function to delete multiple items
    fun deleteItems(itemIds: List<Int>) {
        viewModelScope.launch {
            try {
                repository.deleteItemsByIds(itemIds) // Call the repository method to delete items
            } catch (e: Exception) {
                _error.value = "Error deleting items: ${e.message}"
            }
        }
    }

    // Function to get a single item by ID
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