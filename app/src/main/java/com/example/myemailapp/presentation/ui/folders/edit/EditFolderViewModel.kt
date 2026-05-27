package com.example.myemailapp.presentation.ui.folders.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.FoldersRepository
import com.example.myemailapp.data.service.UpdateFolderStatusService
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.ProcessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditFolderViewModel(
    savedStateHandle: SavedStateHandle,
    private val foldersRepository: FoldersRepository,
    private val updateFolderStatusService: UpdateFolderStatusService
) : ViewModel() {

    private val folderId: String = savedStateHandle.get<String>("folderId") ?: ""

    private val _state = MutableStateFlow(EditFolderState())
    val state = _state.asStateFlow()

    init {
        loadFolder()
    }

    private fun loadFolder() {
        if (folderId.isEmpty()) {
            _state.update { it.copy(processState = ProcessState.Failure) }
            return
        }

        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            foldersRepository.getFolderById(folderId).fold(
                onSuccess = { folder ->
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                processState = ProcessState.Initial,
                                folder = folder,
                                name = folder.name
                            )
                        }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(processState = ProcessState.Failure) }
                    }
                }
            )
        }
    }

    fun updateFolderName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun saveFolder() {
        val folder = _state.value.folder ?: return
        val newName = _state.value.name.trim()

        if (newName.isEmpty()) {
            return
        }

        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            val updatedFolder = folder.copy(name = newName)
            foldersRepository.updateFolder(updatedFolder).fold(
                onSuccess = {
                    updateFolderStatusService.emitUpdatedFolder(updatedFolder)
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(processState = ProcessState.Success) }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(processState = ProcessState.Failure) }
                    }
                }
            )
        }
    }
}

data class EditFolderState(
    val processState: ProcessState = ProcessState.Initial,
    val folder: Folder? = null,
    val name: String = ""
)
