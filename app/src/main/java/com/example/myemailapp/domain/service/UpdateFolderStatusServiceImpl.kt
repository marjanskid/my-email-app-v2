package com.example.myemailapp.domain.service

import com.example.myemailapp.data.service.FolderUpdateEvent
import com.example.myemailapp.data.service.UpdateFolderStatusService
import com.example.myemailapp.domain.model.Folder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class UpdateFolderStatusServiceImpl : UpdateFolderStatusService {

    private val _updateFolderStatusEvents = MutableSharedFlow<FolderUpdateEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override val updateFolderStatusEvents: SharedFlow<FolderUpdateEvent> =
        _updateFolderStatusEvents.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun emitUpdatedFolder(folder: Folder) {
        _updateFolderStatusEvents.emit(FolderUpdateEvent.Updated(folder))
        _updateFolderStatusEvents.resetReplayCache()
    }
}
