package com.example.myemailapp.data.service

import com.example.myemailapp.domain.model.CreateFolderResult
import kotlinx.coroutines.flow.SharedFlow

interface CreateFolderStatusService {
    val createFolderStatusEvents: SharedFlow<CreateFolderResult>
    suspend fun emitStatus(result: CreateFolderResult)
}
