package com.techvertex.blurvision.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvertex.blurvision.core.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashNavigationDestination {
    data object Intro : SplashNavigationDestination
    data object Home : SplashNavigationDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SplashNavigationDestination>()
    val navigationEvent: SharedFlow<SplashNavigationDestination> = _navigationEvent.asSharedFlow()

    init {
        checkIntroStatus()
    }

    private fun checkIntroStatus() {
        viewModelScope.launch {
            delay(2500)

            val isPassIntro = dataStoreManager.isPassIntro.first()

            if (isPassIntro) {
                _navigationEvent.emit(SplashNavigationDestination.Home)
            } else {
                _navigationEvent.emit(SplashNavigationDestination.Intro)
            }
        }
    }
}
