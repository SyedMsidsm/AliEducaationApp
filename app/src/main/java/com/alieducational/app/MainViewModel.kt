package com.alieducational.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alieducational.app.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val isFirstLaunch: Boolean = true,
    val isLockEnabled: Boolean = false,
    val isDarkTheme: Boolean = true,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isFirstLaunch = appPreferences.isFirstLaunch.first()
            val isLockEnabled = appPreferences.isLockEnabled.first()
            val themeMode = appPreferences.themeMode.first()

            _uiState.value = MainUiState(
                isLoading = false,
                isFirstLaunch = isFirstLaunch,
                isLockEnabled = isLockEnabled,
                isDarkTheme = themeMode == "dark",
            )
        }
    }
}
