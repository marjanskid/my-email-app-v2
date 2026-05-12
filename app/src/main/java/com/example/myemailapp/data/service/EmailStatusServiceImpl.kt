package com.example.myemailapp.data.service

import com.example.myemailapp.data.service.EmailStatusService
import com.example.myemailapp.domain.model.EmailResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EmailStatusServiceImpl : EmailStatusService {

    private val _emailStatusEvents = MutableSharedFlow<EmailResult>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override val emailStatusEvents: SharedFlow<EmailResult> = _emailStatusEvents.asSharedFlow()

    override suspend fun emitStatus(result: EmailResult) {
        if (result != EmailResult.None) {
            _emailStatusEvents.emit(result)
        }
    }
}
