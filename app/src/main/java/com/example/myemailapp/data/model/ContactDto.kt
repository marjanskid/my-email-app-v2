package com.example.myemailapp.data.model

import com.google.firebase.Timestamp

data class ContactDto(
    val id: String = "",
    val name: String = "",
    val displayName: String = "",
    val email: String = "",
    val format: String = "PLAIN",
    val photoBase64: String = "",
    val createdAt: Timestamp? = null
)
