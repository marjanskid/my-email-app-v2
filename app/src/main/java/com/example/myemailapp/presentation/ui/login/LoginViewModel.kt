package com.example.myemailapp.presentation.ui.login

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

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun updateEmail(value: String) {
        _state.update { previous -> previous.copy(email = value) }
    }

    fun updatePassword(value: String) {
        _state.update { previous -> previous.copy(password = value) }
    }

    fun login() {
        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            val email = _state.value.email.trim()
            val password = _state.value.password.trim()

            authRepository.login(email = email, password = password).fold(
                onSuccess = { data ->
                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                userLoggedIn = true,
                                processState = ProcessState.Success
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
        _state.update { previous -> previous.copy(processState = ProcessState.Initial) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val userLoggedIn: Boolean = false,
    val processState: ProcessState = ProcessState.Initial,
    val showPassword: Boolean = false
)