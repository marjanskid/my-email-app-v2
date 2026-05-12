package com.example.myemailapp.data.model

import com.google.firebase.Timestamp

data class UserDto(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null
)
