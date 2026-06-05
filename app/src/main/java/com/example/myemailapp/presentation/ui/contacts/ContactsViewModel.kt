package com.example.myemailapp.presentation.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.ContactRepository
import com.example.myemailapp.data.service.ContactStatusEvent
import com.example.myemailapp.data.service.ContactStatusService
import com.example.myemailapp.domain.model.Contact
import com.example.myemailapp.domain.model.ProcessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactsState(
    val processState: ProcessState = ProcessState.Loading,
    val contacts: List<Contact> = emptyList()
)

class ContactsViewModel(
    private val contactRepository: ContactRepository,
    private val contactStatusService: ContactStatusService
) : ViewModel() {
    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    init {
        loadContacts()
        subscribeToContactEvents()
    }

    private fun subscribeToContactEvents() {
        viewModelScope.launch {
            contactStatusService.events.collect { event ->
                when (event) {
                    is ContactStatusEvent.Created -> addContactLocally(event.contact)
                    is ContactStatusEvent.Updated -> updateContactLocally(event.contact)
                }
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = contactRepository.getContacts()
            result.fold(
                onSuccess = { contacts ->
                    withContext(Dispatchers.Main) {
                        _state.value = ContactsState(
                            processState = ProcessState.Success,
                            contacts = contacts
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

    private fun addContactLocally(contact: Contact) {
        _state.update { current ->
            current.copy(
                contacts = (current.contacts + contact).sortedBy { it.name }
            )
        }
    }

    private fun updateContactLocally(contact: Contact) {
        _state.update { current ->
            current.copy(
                contacts = current.contacts.map { if (it.id == contact.id) contact else it }
            )
        }
    }
}
