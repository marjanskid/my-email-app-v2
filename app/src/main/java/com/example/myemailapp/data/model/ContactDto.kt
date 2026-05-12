package com.example.myemailapp.data.model

import com.google.firebase.Timestamp

data class ContactDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null
)
