package com.example.myemailapp.data.model

data class EmailDocumentDto(
    val email: EmailDto = EmailDto(),
    val attachments: List<AttachmentDto> = emptyList(),
    val recipients: List<String> = emptyList()
)
