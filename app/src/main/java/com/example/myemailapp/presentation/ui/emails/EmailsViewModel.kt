package com.example.myemailapp.presentation.ui.emails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.EmailRepository
import com.example.myemailapp.data.service.EmailStatusService
import com.example.myemailapp.domain.model.EmailResult
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.db.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailsViewModel(
    private val emailStatusService: EmailStatusService,
    private val emailRepository: EmailRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmailsState())
    val state = _state.asStateFlow()

    private val _emailStatusEvent = MutableSharedFlow<EmailResult>(replay = 1)
    val emailStatusEvent = _emailStatusEvent.asSharedFlow()

    // Pagination state - kept separate from UI state for cleaner management
    private var lastDocumentId: String? = null
    private var hasMoreEmails: Boolean = true
    private var isLoadingMore: Boolean = false

    init {
        viewModelScope.launch {
            emailStatusService.emailStatusEvents.collect { result ->
                _emailStatusEvent.emit(result)
            }
        }
        loadEmails()
    }

    /**
     * Loads the first page of emails, resetting pagination state.
     */
    fun loadEmails() {
        // Reset pagination state
        lastDocumentId = null
        hasMoreEmails = true
        isLoadingMore = false

        _state.update { it.copy(processState = ProcessState.Loading, hasMore = true) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.getReceivedEmailsPaginated(
                pageSize = EmailRepository.DEFAULT_PAGE_SIZE,
                lastDocumentId = null
            ).fold(
                onSuccess = { result ->
                    lastDocumentId = result.lastDocumentId
                    hasMoreEmails = result.hasMore

                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                processState = ProcessState.Success,
                                emails = result.emails,
                                hasMore = result.hasMore,
                                errorMessage = null,
                                isRefreshing = false
                            )
                        }
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                processState = ProcessState.Failure,
                                errorMessage = error.message ?: "Failed to load emails",
                                isRefreshing = false
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Loads the next page of emails (called when scrolling near the end).
     */
    fun loadMoreEmails() {
        // Prevent multiple simultaneous loads and loading when no more data
        if (isLoadingMore || !hasMoreEmails || lastDocumentId == null) return

        isLoadingMore = true
        _state.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.getReceivedEmailsPaginated(
                pageSize = EmailRepository.DEFAULT_PAGE_SIZE,
                lastDocumentId = lastDocumentId
            ).fold(
                onSuccess = { result ->
                    lastDocumentId = result.lastDocumentId
                    hasMoreEmails = result.hasMore
                    isLoadingMore = false

                    withContext(Dispatchers.Main) {
                        _state.update { currentState ->
                            currentState.copy(
                                emails = currentState.emails + result.emails,
                                hasMore = result.hasMore,
                                isLoadingMore = false
                            )
                        }
                    }
                },
                onFailure = { error ->
                    isLoadingMore = false
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                isLoadingMore = false,
                                errorMessage = error.message ?: "Failed to load more emails"
                            )
                        }
                    }
                }
            )
        }
    }

    fun refreshEmails() {
        _state.update { it.copy(isRefreshing = true) }
        loadEmails()
    }

    fun updateSearchBarVisibility() {
        _state.update { previous -> previous.copy(searchBarVisible = !previous.searchBarVisible) }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun clearSearch() {
        _state.update { it.copy(searchQuery = "", searchBarVisible = false) }
    }

    fun getFilteredEmails(): List<Email> {
        val query = state.value.searchQuery.lowercase().trim()
        if (query.isEmpty()) return state.value.emails

        return state.value.emails.filter { email ->
            email.subject.lowercase().contains(query) ||
            email.content.lowercase().contains(query) ||
            email.from.lowercase().contains(query) ||
            email.to.lowercase().contains(query) ||
            email.cc.lowercase().contains(query) ||
            email.bcc.lowercase().contains(query) ||
            email.tags.any { it.name.lowercase().contains(query) }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun clearEmailStatusEvent() {
        _emailStatusEvent.resetReplayCache()
    }

    fun toggleStar(emailId: String, currentStarred: Boolean) {
        // Optimistic update
        _state.update { currentState ->
            currentState.copy(
                emails = currentState.emails.map { email ->
                    if (email.id == emailId) {
                        email.copy(isStarred = !currentStarred)
                    } else {
                        email
                    }
                }
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.toggleStar(emailId, !currentStarred).fold(
                onSuccess = {
                    // Already optimistically updated
                },
                onFailure = {
                    // Revert on failure
                    withContext(Dispatchers.Main) {
                        _state.update { currentState ->
                            currentState.copy(
                                emails = currentState.emails.map { email ->
                                    if (email.id == emailId) {
                                        email.copy(isStarred = currentStarred)
                                    } else {
                                        email
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Updates a single email locally without full refresh.
     * Used for optimistic updates when returning from ViewEmailScreen.
     */
    fun updateEmailLocally(updatedEmail: Email) {
        _state.update { currentState ->
            currentState.copy(
                emails = currentState.emails.map { email ->
                    if (email.id == updatedEmail.id) updatedEmail else email
                }
            )
        }
    }

    /**
     * Removes a single email locally without full refresh.
     * Used for optimistic updates when an email is deleted in ViewEmailScreen.
     */
    fun removeEmailLocally(emailId: String) {
        _state.update { currentState ->
            currentState.copy(
                emails = currentState.emails.filter { it.id != emailId }
            )
        }
    }
}

data class EmailsState(
    val processState: ProcessState = ProcessState.Initial,
    val searchBarVisible: Boolean = false,
    val searchQuery: String = "",
    val emails: List<Email> = emptyList(),
    val errorMessage: String? = null,
    // Pagination state
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    // Pull-to-refresh state
    val isRefreshing: Boolean = false
)