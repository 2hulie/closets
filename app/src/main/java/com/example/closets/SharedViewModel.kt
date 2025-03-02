package com.example.closets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.closets.ui.items.ClothingItem

class SharedViewModel : ViewModel() {
    private val _checkedItems = MutableLiveData<List<ClothingItem>>(emptyList())
    val checkedItems: LiveData<List<ClothingItem>> = _checkedItems

    fun setCheckedItems(items: List<ClothingItem>) {
        _checkedItems.value = items
    }

    fun addCheckedItem(item: ClothingItem) {
        val currentItems = _checkedItems.value?.toMutableList() ?: mutableListOf()
        if (!currentItems.contains(item)) {
            currentItems.add(item)
            _checkedItems.value = currentItems
        }
    }

    fun removeCheckedItem(item: ClothingItem) {
        val currentItems = _checkedItems.value?.toMutableList() ?: mutableListOf()
        currentItems.removeIf { it.id == item.id }
        _checkedItems.value = currentItems
    }

    fun clearCheckedItems() {
        _checkedItems.value = emptyList()
    }
}