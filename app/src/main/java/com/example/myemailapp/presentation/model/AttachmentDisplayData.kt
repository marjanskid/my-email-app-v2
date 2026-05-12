package com.example.myemailapp.presentation.model

import android.graphics.Bitmap
import com.example.myemailapp.domain.model.Attachment

data class AttachmentDisplayData(
    val attachment: Attachment,
    val thumbnail: Bitmap?,
    val fullImage: Bitmap?,
    val fileSize: String,
    val fileTypeLabel: String,
    val isImage: Boolean
)
