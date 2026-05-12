package com.example.myemailapp.presentation.ui.emails.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.EmailRepository
import com.example.myemailapp.data.service.EmailStatusService
import com.example.myemailapp.domain.model.Attachment
import com.example.myemailapp.presentation.model.AttachmentDisplayData
import com.example.myemailapp.domain.model.db.Email
import com.example.myemailapp.domain.model.EmailResult
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateEmailViewModel(
    private val authRepository: AuthRepository,
    private val emailRepository: EmailRepository,
    private val emailStatusService: EmailStatusService
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEmailState())
    val state = _state.asStateFlow()

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 512_000 // 500 KB
        private const val MAX_TOTAL_SIZE_BYTES = 900_000 // 900 KB total for all attachments
    }

    fun updateTo(value: String) {
        _state.update { previous -> previous.copy(to = value) }
    }

    fun updateCc(value: String) {
        _state.update { previous -> previous.copy(cc = value) }
    }

    fun updateBcc(value: String) {
        _state.update { previous -> previous.copy(bcc = value) }
    }

    fun updateSubject(value: String) {
        _state.update { previous -> previous.copy(subject = value) }
    }

    fun updateContent(value: String) {
        _state.update { previous -> previous.copy(content = value) }
    }

    fun addAttachment(attachment: Attachment) {
        // Calculate file size from base64 string (base64 is ~4/3 the size of original)
        val estimatedFileSize = (attachment.dataBase64.length * 3) / 4

        // Check individual file size
        if (estimatedFileSize > MAX_FILE_SIZE_BYTES) {
            val fileSizeKB = estimatedFileSize / 1024
            _state.update {
                it.copy(errorMessage = "File '${attachment.name}' is too large (${fileSizeKB}KB). Maximum size is 500KB.")
            }
            return
        }

        // Calculate total size with new attachment
        val currentTotalSize = _state.value.attachments.sumOf { (it.attachment.dataBase64.length * 3) / 4 }
        val newTotalSize = currentTotalSize + estimatedFileSize

        if (newTotalSize > MAX_TOTAL_SIZE_BYTES) {
            _state.update {
                it.copy(errorMessage = "Total attachment size would exceed limit. Maximum total size is 900KB.")
            }
            return
        }

        val isImage = isImageMimeType(attachment.type)
        val displayData = AttachmentDisplayData(
            attachment = attachment,
            thumbnail = if (isImage) {
                decodeBase64ToBitmap(attachment.dataBase64, maxWidth = 100, maxHeight = 100)
            } else null,
            fullImage = null,
            fileSize = formatFileSize(calculateBase64Size(attachment.dataBase64)),
            fileTypeLabel = getFileTypeLabel(attachment.type),
            isImage = isImage
        )

        _state.update { it.copy(attachments = it.attachments + displayData, errorMessage = null) }
    }

    fun removeAttachment(attachmentId: String) {
        _state.update { state -> state.copy(attachments = state.attachments.filterNot { it.attachment.id == attachmentId }) }
    }

    fun sendEmail() {
        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.send(email = emailFromState()).fold(
                onSuccess = { emailId ->
                    emailStatusService.emitStatus(EmailResult.Sent)
                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                processState = ProcessState.Success,
                                emailResult = EmailResult.Sent
                            )
                        }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update { previous -> previous.copy(processState = ProcessState.Failure) }
                    }
                }
            )
        }
    }

    fun saveDraft() {
        if (!isThereAnyEmailFieldWithValue()) {
            _state.update { previous ->
                previous.copy(
                    processState = ProcessState.Success,
                    emailResult = EmailResult.None
                )
            }
            return
        }

        _state.update { previous -> previous.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.saveDraft(email = emailFromState()).fold(
                onSuccess = { data ->
                    emailStatusService.emitStatus(EmailResult.DraftSaved)
                    withContext(Dispatchers.Main) {
                        _state.update { previous ->
                            previous.copy(
                                processState = ProcessState.Success,
                                emailResult = EmailResult.DraftSaved
                            )
                        }
                    }
                },
                onFailure = { failure ->
                    withContext(Dispatchers.Main) {
                        _state.update { previous -> previous.copy(processState = ProcessState.Failure) }
                    }
                }
            )
        }

    }

    fun initAsReply(originalEmailId: String) {
        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.getEmailById(originalEmailId).fold(
                onSuccess = { originalEmail ->
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                to = originalEmail.from,
                                bcc = "",  // BCC should be empty on reply
                                subject = if (originalEmail.subject.startsWith("Re: "))
                                    originalEmail.subject
                                else
                                    "Re: ${originalEmail.subject}",
                                content = "\n\n--- Original Message ---\n" +
                                        "From: ${originalEmail.from}\n" +
                                        "Date: ${originalEmail.dateTime}\n\n" +
                                        originalEmail.content,
                                processState = ProcessState.Initial
                            )
                        }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                processState = ProcessState.Failure,
                                errorMessage = "Failed to load original email for reply"
                            )
                        }
                    }
                }
            )
        }
    }

    fun initAsReplyAll(originalEmailId: String) {
        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.getEmailById(originalEmailId).fold(
                onSuccess = { originalEmail ->
                    val currentUserEmail = authRepository.currentUser?.email?.lowercase() ?: ""

                    // To: Original sender (unless it's the current user)
                    val toRecipient = if (originalEmail.from.lowercase() != currentUserEmail) {
                        originalEmail.from
                    } else {
                        ""
                    }

                    // CC: Original To + Original CC, excluding current user
                    val ccRecipients = buildList {
                        originalEmail.to.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && it.lowercase() != currentUserEmail }
                            .forEach { add(it) }
                        originalEmail.cc.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && it.lowercase() != currentUserEmail }
                            .forEach { add(it) }
                    }
                        .distinct()
                        .joinToString(", ")

                    // BCC: Never included (original BCC recipients are hidden)

                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                to = toRecipient,
                                cc = ccRecipients,
                                bcc = "",  // BCC always empty on reply/reply all
                                showCcBcc = ccRecipients.isNotEmpty(),  // Show CC field if has content
                                subject = if (originalEmail.subject.startsWith("Re: "))
                                    originalEmail.subject
                                else
                                    "Re: ${originalEmail.subject}",
                                content = "\n\n--- Original Message ---\n" +
                                        "From: ${originalEmail.from}\n" +
                                        "Date: ${originalEmail.dateTime}\n\n" +
                                        originalEmail.content,
                                processState = ProcessState.Initial
                            )
                        }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                processState = ProcessState.Failure,
                                errorMessage = "Failed to load original email for reply all"
                            )
                        }
                    }
                }
            )
        }
    }

    fun initAsForward(originalEmailId: String) {
        _state.update { it.copy(processState = ProcessState.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            emailRepository.getEmailById(originalEmailId).fold(
                onSuccess = { originalEmail ->
                    // Convert original attachments to display data
                    val attachmentDisplayData = originalEmail.attachments.map { attachment ->
                        createAttachmentDisplayData(attachment)
                    }

                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                to = "",
                                cc = "",
                                bcc = "",
                                subject = if (originalEmail.subject.startsWith("Fwd: "))
                                    originalEmail.subject
                                else
                                    "Fwd: ${originalEmail.subject}",
                                content = "\n\n--- Forwarded Message ---\n" +
                                        "From: ${originalEmail.from}\n" +
                                        "To: ${originalEmail.to}\n" +
                                        "Date: ${originalEmail.dateTime}\n" +
                                        "Subject: ${originalEmail.subject}\n\n" +
                                        originalEmail.content,
                                attachments = attachmentDisplayData,
                                processState = ProcessState.Initial
                            )
                        }
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                processState = ProcessState.Failure,
                                errorMessage = "Failed to load original email for forward"
                            )
                        }
                    }
                }
            )
        }
    }

    private fun createAttachmentDisplayData(attachment: Attachment): AttachmentDisplayData {
        val isImage = isImageMimeType(attachment.type)
        return AttachmentDisplayData(
            attachment = attachment,
            thumbnail = if (isImage) {
                decodeBase64ToBitmap(attachment.dataBase64, maxWidth = 100, maxHeight = 100)
            } else null,
            fullImage = null,
            fileSize = formatFileSize(calculateBase64Size(attachment.dataBase64)),
            fileTypeLabel = getFileTypeLabel(attachment.type),
            isImage = isImage
        )
    }

    fun isThereAnyEmailFieldWithValue(): Boolean {
        return !_state.value.to.trim().isEmpty()
                || !_state.value.cc.trim().isEmpty()
                || !_state.value.bcc.trim().isEmpty()
                || !_state.value.subject.trim().isEmpty()
                || !_state.value.content.trim().isEmpty()
                || _state.value.attachments.isNotEmpty()
    }

    fun emailFromState(): Email {
        return Email.toEmail(
            to = _state.value.to.trim(),
            cc = _state.value.cc.trim(),
            bcc = _state.value.bcc.trim(),
            subject = _state.value.subject.trim(),
            content = _state.value.content,
            attachments = _state.value.attachments.map { it.attachment }
        )
    }


    fun resetErrorState() {
        _state.update { it.copy(processState = ProcessState.Initial) }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun resetEmailResult() {
        _state.update { it.copy(emailResult = EmailResult.None) }
    }

    fun toggleCcBcc() {
        _state.update { it.copy(showCcBcc = !it.showCcBcc) }
    }
}

data class CreateEmailState(
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val content: String = "",
    val attachments: List<AttachmentDisplayData> = emptyList(),
    val showCcBcc: Boolean = false,
    val userLoggedOut: Boolean = false, // maybe?
    val searchBarVisible: Boolean = false,
    val errorOccurred: Boolean = false,
    val processState: ProcessState = ProcessState.Initial,
    val errorMessage: String? = null,
    val emailResult: EmailResult = EmailResult.None
)