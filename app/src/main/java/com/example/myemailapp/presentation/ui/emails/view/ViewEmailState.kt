package com.example.myemailapp.presentation.ui.emails.view

import com.example.myemailapp.domain.model.Attachment
import com.example.myemailapp.presentation.model.AttachmentDisplayData
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.db.Email

data class ViewEmailState(
    val email: Email? = null,
    val attachmentDisplayDataList: List<AttachmentDisplayData> = emptyList(),
    val selectedAttachment: AttachmentDisplayData? = null,
    val processState: ProcessState = ProcessState.Initial,
    val errorMessage: String? = null,
    val emailDeleted: Boolean = false,
    val savingAttachmentIds: Set<String> = emptySet(),
    val savedAttachmentIds: Set<String> = emptySet(),
    val savingAllAttachments: Boolean = false,
    val saveAllProgress: Int = 0,
    val saveAllTotal: Int = 0,
    val attachmentPendingSave: Attachment? = null,
    val showTagDialog: Boolean = false,
    val isTogglingTag: Boolean = false
)
