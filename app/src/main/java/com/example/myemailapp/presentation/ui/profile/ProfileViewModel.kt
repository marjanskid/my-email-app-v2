package com.example.myemailapp.presentation.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.AuthRepository
import com.example.myemailapp.domain.model.ProcessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmailsState())
    val state = _state.asStateFlow()

    fun logout() {
        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout().fold(
                onSuccess = { data ->
                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                processState = ProcessState.Success,
                                userLoggedOut = true
                            )
                        }
                    }
                },
                onFailure = { failure ->
                    withContext(Dispatchers.Main) {
                        _state.update { previous -> previous.copy(processState = ProcessState.Failure) }
                    }
                }
            )
        }
    }

    fun resetProcessState() {
        _state.update { it.copy(processState = ProcessState.Initial) }
    }

    fun updateSearchBarVisibility() {
        _state.update { previous -> previous.copy(searchBarVisible = !previous.searchBarVisible) }
    }
}

data class EmailsState(
    val userLoggedOut: Boolean = false,
    val processState: ProcessState = ProcessState.Initial,
    val searchBarVisible: Boolean = false,
)