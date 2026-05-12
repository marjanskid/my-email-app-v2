package com.example.myemailapp.presentation.ui.folders.view_all

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.FoldersRepository
import com.example.myemailapp.data.service.CreateFolderStatusService
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.ProcessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoldersViewModel(
    private val foldersRepository: FoldersRepository,
    private val createFolderStatusService: CreateFolderStatusService,
) : ViewModel() {
    private val _state = MutableStateFlow(FoldersState())
    val state = _state.asStateFlow()

    init {
        getFolders()

        viewModelScope.launch(Dispatchers.IO) {
            createFolderStatusService.createFolderStatusEvents.collect {
                getFolders()
            }
        }
    }

    fun getFolders() {
        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            foldersRepository.getFolders().fold(
                onSuccess = { data ->
                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                processState = ProcessState.Success,
                                folders = data
                            )
                        }
                    }
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

data class FoldersState(
    val processState: ProcessState = ProcessState.Initial,
    val folders: List<Folder> = emptyList(),
)