package com.solutions.alphil.zambiajobalerts.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val text = MutableLiveData("This is home fragment")

    fun getText(): LiveData<String> = text
}
