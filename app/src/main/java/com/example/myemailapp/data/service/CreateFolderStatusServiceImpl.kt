package com.example.myemailapp.domain.service

import com.example.myemailapp.data.service.CreateFolderStatusService
import com.example.myemailapp.domain.model.CreateFolderResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CreateFolderStatusServiceImpl : CreateFolderStatusService {

    private val _createFolderStatusEvents = MutableSharedFlow<CreateFolderResult>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override val createFolderStatusEvents: SharedFlow<CreateFolderResult> =
        _createFolderStatusEvents.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun emitStatus(result: CreateFolderResult) {
        if (result == CreateFolderResult.Success) {
            _createFolderStatusEvents.emit(result)

            _createFolderStatusEvents.resetReplayCache()
        }
    }
}
