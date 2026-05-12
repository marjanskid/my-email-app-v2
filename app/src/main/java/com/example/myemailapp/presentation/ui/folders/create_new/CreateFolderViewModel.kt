package com.example.myemailapp.presentation.ui.folders.create_new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.FoldersRepository
import com.example.myemailapp.data.service.CreateFolderStatusService
import com.example.myemailapp.domain.model.CreateFolderResult
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.ProcessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateFolderViewModel(
    private val foldersRepository: FoldersRepository,
    private val createFolderStatusService: CreateFolderStatusService,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateFolderState())
    val state = _state.asStateFlow()

    fun updateFolderName(name: String) {
        _state.update { previous -> previous.copy(name = name) }
    }

    fun createFolder() {
        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            val newFolder = Folder(name = _state.value.name)

            foldersRepository.createFolder(folder = newFolder).fold(
                onSuccess = {
                    createFolderStatusService.emitStatus(CreateFolderResult.Success)
                    _state.update { previous -> previous.copy(processState = ProcessState.Success) }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update { previous -> previous.copy(processState = ProcessState.Failure) }
                    }
                }
            )
        }
    }
}

data class CreateFolderState(
    val processState: ProcessState = ProcessState.Initial,
    val name: String = "",
)