package com.solutions.alphil.zambiajobalerts.ui.slideshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SlideshowViewModel : ViewModel() {
    private val text = MutableLiveData("This is slideshow fragment")

    fun getText(): LiveData<String> = text
}
