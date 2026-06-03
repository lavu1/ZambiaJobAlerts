package com.solutions.alphil.zambiajobalerts.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GalleryViewModel : ViewModel() {
    private val text = MutableLiveData("This is gallery fragment")

    fun getText(): LiveData<String> = text
}
