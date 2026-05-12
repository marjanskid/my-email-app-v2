package com.example.myemailapp.presentation.ui.emails

import androidx.lifecycle.ViewModel
import com.example.myemailapp.presentation.model.AttachmentDisplayData
import com.example.myemailapp.presentation.ui.emails.create.decodeBase64ToBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AttachmentViewModel : ViewModel() {

    private val _state = MutableStateFlow(AttachmentViewerState())
    val state = _state.asStateFlow()

    fun selectAttachment(displayData: AttachmentDisplayData) {
        val fullImage = if (displayData.isImage) {
            decodeBase64ToBitmap(displayData.attachment.dataBase64, maxWidth = 2048, maxHeight = 2048)
        } else null
        _state.update { it.copy(selectedAttachment = displayData.copy(fullImage = fullImage)) }
    }

    fun clearSelectedAttachment() {
        _state.update { it.copy(selectedAttachment = null) }
    }
}

data class AttachmentViewerState(
    val selectedAttachment: AttachmentDisplayData? = null
)
