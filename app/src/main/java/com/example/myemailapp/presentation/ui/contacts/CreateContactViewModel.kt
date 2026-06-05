package com.example.myemailapp.presentation.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.ContactRepository
import com.example.myemailapp.data.service.ContactStatusEvent
import com.example.myemailapp.data.service.ContactStatusService
import com.example.myemailapp.domain.model.Contact
import com.example.myemailapp.domain.model.ContactFormat
import com.example.myemailapp.domain.model.ProcessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreateContactState(
    val processState: ProcessState = ProcessState.Initial,
    val name: String = "",
    val displayName: String = "",
    val email: String = "",
    val format: ContactFormat = ContactFormat.PLAIN,
    val photoBase64: String = ""
)

class CreateContactViewModel(
    private val contactRepository: ContactRepository,
    private val contactStatusService: ContactStatusService
) : ViewModel() {
    private val _state = MutableStateFlow(CreateContactState())
    val state: StateFlow<CreateContactState> = _state.asStateFlow()

    fun updateName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun updateDisplayName(displayName: String) {
        _state.update { it.copy(displayName = displayName) }
    }

    fun updateEmail(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun updateFormat(format: ContactFormat) {
        _state.update { it.copy(format = format) }
    }

    fun updatePhoto(photoBase64: String) {
        _state.update { it.copy(photoBase64 = photoBase64) }
    }

    fun createContact() {
        val current = _state.value
        if (current.name.isBlank() || current.email.isBlank()) return

        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            val newContact = Contact(
                name = current.name,
                displayName = current.displayName,
                email = current.email,
                format = current.format,
                photoBase64 = current.photoBase64
            )
            contactRepository.addContact(newContact).fold(
                onSuccess = { contactId ->
                    withContext(Dispatchers.Main) {
                        val createdContact = newContact.copy(id = contactId)
                        contactStatusService.emitEvent(ContactStatusEvent.Created(createdContact))
                        _state.update { it.copy(processState = ProcessState.Success) }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(processState = ProcessState.Failure) }
                    }
                }
            )
        }
    }
}
