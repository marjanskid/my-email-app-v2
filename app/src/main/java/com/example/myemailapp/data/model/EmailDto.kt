package com.example.myemailapp.data.model

import com.google.firebase.Timestamp

data class EmailDto(
    val id: String = "",
    val from: String = "",
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val content: String = "",
    val dateTime: Timestamp? = null,
    val status: String = "",
    val folderId: String? = null
)
