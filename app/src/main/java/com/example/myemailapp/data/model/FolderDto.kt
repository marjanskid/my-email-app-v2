package com.example.myemailapp.data.model

import com.google.firebase.Timestamp

data class FolderDto(
    val id: String = "",
    val name: String = "",
    val parentId: String? = null,
    val createdAt: Timestamp? = null
)
