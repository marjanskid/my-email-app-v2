package com.example.myemailapp.data.service

import com.example.myemailapp.domain.model.Folder
import kotlinx.coroutines.flow.SharedFlow

sealed class FolderUpdateEvent {
    data class Updated(val folder: Folder) : FolderUpdateEvent()
}

interface UpdateFolderStatusService {
    val updateFolderStatusEvents: SharedFlow<FolderUpdateEvent>
    suspend fun emitUpdatedFolder(folder: Folder)
}
