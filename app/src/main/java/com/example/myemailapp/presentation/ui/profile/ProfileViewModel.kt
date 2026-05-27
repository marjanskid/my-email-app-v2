package com.example.myemailapp.presentation.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.AuthRepository
import com.example.myemailapp.data.repository.FoldersRepository
import com.example.myemailapp.data.repository.RulesRepository
import com.example.myemailapp.data.repository.UserRepository
import com.example.myemailapp.domain.model.Condition
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.Operation
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.Rule
import com.example.myemailapp.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val rulesRepository: RulesRepository,
    private val foldersRepository: FoldersRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch(Dispatchers.IO) {
            val userDeferred = async { userRepository.getUserProfile() }
            val rulesDeferred = async { rulesRepository.getRules() }
            val foldersDeferred = async { foldersRepository.getFolders() }

            val userResult = userDeferred.await()
            val rulesResult = rulesDeferred.await()
            val foldersResult = foldersDeferred.await()

            val user = userResult.getOrElse { authRepository.currentUser }

            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        user = user,
                        rules = rulesResult.getOrDefault(emptyList()),
                        folders = foldersResult.getOrDefault(emptyList())
                    )
                }
            }
        }
    }

    fun createRule(
        condition: Condition,
        conditionValue: String,
        operation: Operation,
        destinationFolder: Folder?
    ) {
        if (conditionValue.isBlank()) return
        if (operation != Operation.DELETE && destinationFolder == null) return

        val rule = Rule(
            condition = condition,
            conditionValue = conditionValue.trim(),
            operation = operation,
            destinationFolderId = destinationFolder?.id ?: "",
            destinationFolderName = destinationFolder?.name ?: ""
        )

        viewModelScope.launch(Dispatchers.IO) {
            rulesRepository.createRule(rule).fold(
                onSuccess = { ruleId ->
                    withContext(Dispatchers.Main) {
                        val newRule = rule.copy(id = ruleId)
                        _state.update { state ->
                            state.copy(
                                rules = state.rules + newRule,
                                showAddRuleDialog = false
                            )
                        }
                    }
                },
                onFailure = { failure ->
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(errorMessage = failure.message) }
                    }
                }
            )
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            rulesRepository.deleteRule(ruleId).fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        _state.update { state ->
                            state.copy(rules = state.rules.filter { it.id != ruleId })
                        }
                    }
                },
                onFailure = { failure ->
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(errorMessage = failure.message) }
                    }
                }
            )
        }
    }

    fun showAddRuleDialog() {
        _state.update { it.copy(showAddRuleDialog = true) }
    }

    fun hideAddRuleDialog() {
        _state.update { it.copy(showAddRuleDialog = false) }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun logout() {
        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout().fold(
                onSuccess = { _ ->
                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                processState = ProcessState.Success,
                                userLoggedOut = true
                            )
                        }
                    }
                },
                onFailure = { _ ->
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
}

data class ProfileState(
    val user: User? = null,
    val rules: List<Rule> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val showAddRuleDialog: Boolean = false,
    val errorMessage: String? = null,
    val userLoggedOut: Boolean = false,
    val processState: ProcessState = ProcessState.Initial
)
