package com.example.myemailapp.data.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ContactStatusServiceImpl : ContactStatusService {
    private val _events = MutableSharedFlow<ContactStatusEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override val events: SharedFlow<ContactStatusEvent> = _events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun emitEvent(event: ContactStatusEvent) {
        _events.emit(event)
        _events.resetReplayCache()
    }
}
