package com.example.myemailapp.presentation.ui.contacts

import androidx.lifecycle.SavedStateHandle
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

data class ContactState(
    val processState: ProcessState = ProcessState.Loading,
    val contact: Contact = Contact(),
    val name: String = "",
    val displayName: String = "",
    val email: String = "",
    val format: ContactFormat = ContactFormat.PLAIN,
    val photoBase64: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: Boolean = false
)

class ContactViewModel(
    savedStateHandle: SavedStateHandle,
    private val contactRepository: ContactRepository,
    private val contactStatusService: ContactStatusService
) : ViewModel() {
    private val contactId: String = savedStateHandle.get<String>("contactId") ?: ""
    private val _state = MutableStateFlow(ContactState())
    val state: StateFlow<ContactState> = _state.asStateFlow()

    init {
        loadContact()
    }

    private fun loadContact() {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.getContactById(contactId).fold(
                onSuccess = { contact ->
                    withContext(Dispatchers.Main) {
                        _state.value = ContactState(
                            processState = ProcessState.Success,
                            contact = contact,
                            name = contact.name,
                            displayName = contact.displayName,
                            email = contact.email,
                            format = contact.format,
                            photoBase64 = contact.photoBase64
                        )
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

    fun clearSaveError() {
        _state.update { it.copy(saveError = false) }
    }

    fun updateContact() {
        val current = _state.value
        if (current.name.isBlank() || current.email.isBlank()) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val updatedContact = current.contact.copy(
                name = current.name,
                displayName = current.displayName,
                email = current.email,
                format = current.format,
                photoBase64 = current.photoBase64
            )
            contactRepository.updateContact(updatedContact).fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        contactStatusService.emitEvent(ContactStatusEvent.Updated(updatedContact))
                        _state.update { it.copy(isSaving = false, isSaved = true) }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(isSaving = false, saveError = true) }
                    }
                }
            )
        }
    }
}
