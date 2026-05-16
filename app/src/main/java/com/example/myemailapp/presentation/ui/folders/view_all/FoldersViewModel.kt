package com.example.myemailapp.presentation.ui.folders.view_all

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.EmailRepository
import com.example.myemailapp.data.repository.FoldersRepository
import com.example.myemailapp.data.service.CreateFolderStatusService
import com.example.myemailapp.data.service.FolderUpdateEvent
import com.example.myemailapp.data.service.UpdateFolderStatusService
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.FolderWithCount
import com.example.myemailapp.domain.model.ProcessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoldersViewModel(
    private val foldersRepository: FoldersRepository,
    private val emailRepository: EmailRepository,
    private val createFolderStatusService: CreateFolderStatusService,
    private val updateFolderStatusService: UpdateFolderStatusService,
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

        viewModelScope.launch(Dispatchers.IO) {
            updateFolderStatusService.updateFolderStatusEvents.collect { event ->
                when (event) {
                    is FolderUpdateEvent.Updated -> {
                        withContext(Dispatchers.Main) {
                            updateFolderLocally(event.folder)
                        }
                    }
                }
            }
        }
    }

    private fun updateFolderLocally(updatedFolder:  Folder) {
        _state.update { previous ->
            previous.copy(
                folders = previous.folders.map { folderWithCount ->
                    if (folderWithCount.folder.id == updatedFolder.id) {
                        folderWithCount.copy(folder = updatedFolder)
                    } else {
                        folderWithCount
                    }
                }
            )
        }
    }

    fun getFolders() {
        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            foldersRepository.getFolders().fold(
                onSuccess = { folders ->
                    // Fetch message counts for all folders in parallel
                    val foldersWithCounts = folders.map { folder ->
                        async {
                            val countResult = emailRepository.getEmailCountByFolderId(folder.id)
                            FolderWithCount(
                                folder = folder,
                                messageCount = countResult.getOrDefault(0)
                            )
                        }
                    }.awaitAll()

                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                processState = ProcessState.Success,
                                folders = foldersWithCounts
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
    val folders: List<FolderWithCount> = emptyList(),
)
