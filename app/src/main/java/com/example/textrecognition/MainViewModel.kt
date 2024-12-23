package com.example.textrecognition

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    var currentImageUriStr = MutableLiveData<String?>().apply { value = "" }
    var resultText = MutableLiveData<String?>().apply { value = "" }
    var isProgressbarVisible = MutableLiveData<Boolean>().apply { value = false }
}