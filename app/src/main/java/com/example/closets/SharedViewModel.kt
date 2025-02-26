package com.example.closets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.closets.ui.items.ClothingItem

class SharedViewModel : ViewModel() {
    private val _checkedItems = MutableLiveData<List<ClothingItem>>()
    val checkedItems: LiveData<List<ClothingItem>> get() = _checkedItems

    fun setCheckedItems(items: List<ClothingItem>) {
        _checkedItems.value = items
    }

    fun clearCheckedItems() {
        _checkedItems.value = emptyList()
    }
}