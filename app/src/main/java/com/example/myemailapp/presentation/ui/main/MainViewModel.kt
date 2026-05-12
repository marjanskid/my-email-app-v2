package com.example.myemailapp.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        checkIfUserLoggedIn()
    }

    private fun checkIfUserLoggedIn() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(5000)
            val user = authRepository.currentUser

            withContext(Dispatchers.Main) {
                _state.update { previous ->
                    previous.copy(isLoading = false, userLoggedIn = user != null)
                }
            }
        }
    }
}

data class MainState(
    val isLoading: Boolean = true,
    val userLoggedIn: Boolean = false
)