package com.example.myemailapp.presentation.ui.folders.view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.EmailRepository
import com.example.myemailapp.data.repository.FoldersRepository
import com.example.myemailapp.data.service.FolderUpdateEvent
import com.example.myemailapp.data.service.UpdateFolderStatusService
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.db.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewFolderViewModel(
    savedStateHandle: SavedStateHandle,
    private val foldersRepository: FoldersRepository,
    private val emailRepository: EmailRepository,
    private val updateFolderStatusService: UpdateFolderStatusService
) : ViewModel() {

    private val folderId: String = savedStateHandle.get<String>("folderId") ?: ""

    private val _state = MutableStateFlow(ViewFolderState())
    val state = _state.asStateFlow()

    init {
        loadFolderData()
        subscribeToFolderUpdates()
    }

    private fun subscribeToFolderUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            updateFolderStatusService.updateFolderStatusEvents.collect { event ->
                when (event) {
                    is FolderUpdateEvent.Updated -> {
                        if (event.folder.id == folderId) {
                            withContext(Dispatchers.Main) {
                                updateFolderLocally(event.folder)
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateFolderLocally(updatedFolder: Folder) {
        _state.update { it.copy(folder = updatedFolder) }
    }

    fun loadFolderData() {
        if (folderId.isEmpty()) {
            _state.update { it.copy(processState = ProcessState.Failure) }
            return
        }

        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            // Load folder info
            val folderResult = foldersRepository.getFolderById(folderId)
            if (folderResult.isFailure) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(processState = ProcessState.Failure) }
                }
                return@launch
            }

            // Load emails in this folder
            val emailsResult = emailRepository.getEmailsByFolderId(folderId)

            withContext(Dispatchers.Main) {
                _state.update { previous ->
                    previous.copy(
                        processState = ProcessState.Success,
                        folder = folderResult.getOrNull(),
                        emails = emailsResult.getOrDefault(emptyList())
                    )
                }
            }
        }
    }

    fun toggleStar(emailId: String, currentStarred: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.toggleStar(emailId, !currentStarred).fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                emails = previous.emails.map { email ->
                                    if (email.id == emailId) {
                                        email.copy(isStarred = !currentStarred)
                                    } else {
                                        email
                                    }
                                }
                            )
                        }
                    }
                },
                onFailure = { /* Handle error if needed */ }
            )
        }
    }

    fun updateEmailLocally(updatedEmail: Email) {
        _state.update { previous ->
            previous.copy(
                emails = previous.emails.map { email ->
                    if (email.id == updatedEmail.id) updatedEmail else email
                }
            )
        }
    }

    fun removeEmailLocally(emailId: String) {
        _state.update { previous ->
            previous.copy(
                emails = previous.emails.filter { it.id != emailId }
            )
        }
    }
}

data class ViewFolderState(
    val processState: ProcessState = ProcessState.Initial,
    val folder: Folder? = null,
    val emails: List<Email> = emptyList()
)
