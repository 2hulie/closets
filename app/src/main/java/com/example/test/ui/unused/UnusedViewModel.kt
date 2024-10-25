package com.example.test.ui.unused

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UnusedViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Unused Fragment"
    }
    val text: LiveData<String> = _text
}