package com.example.myemailapp.data.service

import com.example.myemailapp.domain.model.Contact
import kotlinx.coroutines.flow.SharedFlow

interface ContactStatusService {
    val events: SharedFlow<ContactStatusEvent>
    suspend fun emitEvent(event: ContactStatusEvent)
}

sealed class ContactStatusEvent {
    data class Created(val contact: Contact) : ContactStatusEvent()
    data class Updated(val contact: Contact) : ContactStatusEvent()
}
