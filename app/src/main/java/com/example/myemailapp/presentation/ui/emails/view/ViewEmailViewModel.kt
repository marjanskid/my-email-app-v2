package com.example.myemailapp.presentation.ui.emails.view

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.EmailRepository
import com.example.myemailapp.data.service.EmailStatusService
import com.example.myemailapp.domain.model.Attachment
import com.example.myemailapp.domain.model.EmailResult
import com.example.myemailapp.domain.model.Tag
import com.example.myemailapp.presentation.model.AttachmentDisplayData
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.presentation.ui.emails.create.calculateBase64Size
import com.example.myemailapp.presentation.ui.emails.create.decodeBase64ToBitmap
import com.example.myemailapp.presentation.ui.emails.create.formatFileSize
import com.example.myemailapp.presentation.ui.emails.create.getFileTypeLabel
import com.example.myemailapp.presentation.ui.emails.create.isImageMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewEmailViewModel(
    private val emailRepository: EmailRepository,
    private val emailStatusService: EmailStatusService
) : ViewModel() {

    private val _state = MutableStateFlow(ViewEmailState())
    val state = _state.asStateFlow()

    private val _emailStatusEvent = MutableSharedFlow<EmailResult>(replay = 1)
    val emailStatusEvent = _emailStatusEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            emailStatusService.emailStatusEvents.collect { result ->
                _emailStatusEvent.emit(result)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearEmailStatusEvent() {
        _emailStatusEvent.resetReplayCache()
    }

    private fun createAttachmentDisplayData(attachment: Attachment): AttachmentDisplayData {
        val isImage = isImageMimeType(attachment.type)
        return AttachmentDisplayData(
            attachment = attachment,
            thumbnail = if (isImage) decodeBase64ToBitmap(attachment.dataBase64, 100, 100) else null,
            fullImage = null,
            fileSize = formatFileSize(calculateBase64Size(attachment.dataBase64)),
            fileTypeLabel = getFileTypeLabel(attachment.type),
            isImage = isImage
        )
    }

    fun loadEmail(emailId: String) {
        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.getEmailById(emailId).fold(
                onSuccess = { email ->
                    val displayDataList = email.attachments.map { createAttachmentDisplayData(it) }
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(
                            email = email,
                            attachmentDisplayDataList = displayDataList,
                            processState = ProcessState.Success
                        )}
                    }
                    // Auto-mark as read when email is loaded
                    if (!email.isRead) {
                        markAsRead(emailId)
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(
                            processState = ProcessState.Failure,
                            errorMessage = error.message
                        )}
                    }
                }
            )
        }
    }

    private fun markAsRead(emailId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.markAsRead(emailId, true).fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        _state.update { currentState ->
                            currentState.copy(
                                email = currentState.email?.copy(isRead = true)
                            )
                        }
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                processState = ProcessState.Failure,
                                errorMessage = error.message ?: "Failed to mark email as read"
                            )
                        }
                    }
                }
            )
        }
    }

    fun toggleStar() {
        val email = _state.value.email ?: return
        val newStarredState = !email.isStarred

        // Optimistic update
        _state.update { currentState ->
            currentState.copy(
                email = currentState.email?.copy(isStarred = newStarredState)
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.toggleStar(email.id, newStarredState).fold(
                onSuccess = {
                    // Already optimistically updated
                },
                onFailure = {
                    // Revert on failure
                    withContext(Dispatchers.Main) {
                        _state.update { currentState ->
                            currentState.copy(
                                email = currentState.email?.copy(isStarred = !newStarredState)
                            )
                        }
                    }
                }
            )
        }
    }

    fun deleteEmail() {
        val emailId = _state.value.email?.id ?: return
        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.softDeleteEmail(emailId).fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(
                            processState = ProcessState.Success,
                            emailDeleted = true
                        )}
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(
                            processState = ProcessState.Failure,
                            errorMessage = error.message
                        )}
                    }
                }
            )
        }
    }

    fun selectAttachment(displayData: AttachmentDisplayData) {
        val fullImage = if (displayData.isImage) {
            decodeBase64ToBitmap(displayData.attachment.dataBase64, 2048, 2048)
        } else null
        _state.update { it.copy(selectedAttachment = displayData.copy(fullImage = fullImage)) }
    }

    fun clearSelectedAttachment() {
        _state.update { it.copy(selectedAttachment = null) }
    }

    fun resetProcessState() {
        _state.update { it.copy(processState = ProcessState.Initial) }
    }

    fun showTagSelection() {
        _state.update { it.copy(showTagDialog = true) }
    }

    fun hideTagSelection() {
        _state.update { it.copy(showTagDialog = false) }
    }

    fun toggleTag(tag: Tag) {
        val email = _state.value.email ?: return
        val currentTags = email.tags
        val hasTag = currentTags.any { it.id == tag.id }

        _state.update { it.copy(isTogglingTag = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = if (hasTag) {
                emailRepository.removeTag(email.id, tag.id)
            } else {
                emailRepository.addTag(email.id, tag)
            }

            result.fold(
                onSuccess = {
                    // Update local state immediately
                    val updatedTags = if (hasTag) {
                        currentTags.filter { it.id != tag.id }
                    } else {
                        currentTags + tag
                    }

                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                email = email.copy(tags = updatedTags),
                                isTogglingTag = false
                            )
                        }
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                isTogglingTag = false,
                                processState = ProcessState.Failure,
                                errorMessage = "Failed to update tag: ${error.message}"
                            )
                        }
                    }
                }
            )
        }
    }

    fun requestSaveAttachment(displayData: AttachmentDisplayData) {
        _state.update { it.copy(attachmentPendingSave = displayData.attachment) }
    }

    fun saveAttachmentToUri(uri: Uri, attachment: Attachment, context: Context) {
        _state.update {
            it.copy(
                savingAttachmentIds = it.savingAttachmentIds + attachment.id,
                attachmentPendingSave = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decodedBytes = Base64.decode(attachment.dataBase64, Base64.DEFAULT)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(decodedBytes)
                }

                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(
                            savingAttachmentIds = it.savingAttachmentIds - attachment.id,
                            savedAttachmentIds = it.savedAttachmentIds + attachment.id
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(
                            savingAttachmentIds = it.savingAttachmentIds - attachment.id,
                            processState = ProcessState.Failure,
                            errorMessage = "Failed to save ${attachment.name}: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    fun saveAllAttachmentsToFolder(folderUri: Uri, attachments: List<Attachment>, context: Context) {
        _state.update {
            it.copy(
                savingAllAttachments = true,
                saveAllProgress = 0,
                saveAllTotal = attachments.size
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            val failedAttachments = mutableListOf<String>()

            // Convert tree URI to document URI for the parent folder
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )

            attachments.forEachIndexed { index, attachment ->
                try {
                    val documentUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        parentDocUri,
                        attachment.type,
                        attachment.name
                    )

                    documentUri?.let { uri ->
                        val decodedBytes = Base64.decode(attachment.dataBase64, Base64.DEFAULT)

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(decodedBytes)
                        }

                        successCount++
                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(
                                    saveAllProgress = index + 1,
                                    savedAttachmentIds = it.savedAttachmentIds + attachment.id
                                )
                            }
                        }
                    } ?: run {
                        failedAttachments.add(attachment.name)
                    }
                } catch (_: Exception) {
                    failedAttachments.add(attachment.name)
                }
            }

            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        savingAllAttachments = false,
                        processState = if (failedAttachments.isEmpty())
                            ProcessState.Success else ProcessState.Failure,
                        errorMessage = if (failedAttachments.isNotEmpty())
                            "Failed to save: ${failedAttachments.joinToString(", ")}" else null
                    )
                }
            }
        }
    }
}
