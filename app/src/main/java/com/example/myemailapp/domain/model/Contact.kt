package com.example.myemailapp.domain.model

import java.time.Instant

data class Contact(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Instant = Instant.now()
)
