package com.techvertex.obscura.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvertex.obscura.core.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    fun completeIntro(onCompleted: () -> Unit) {
        viewModelScope.launch {
            dataStoreManager.setPassIntro(true)
            onCompleted()
        }
    }
}
