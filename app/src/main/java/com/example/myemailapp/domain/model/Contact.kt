package com.example.myemailapp.domain.model

import java.time.Instant

data class Contact(
    val id: String = "",
    val name: String = "",
    val displayName: String = "",
    val email: String = "",
    val format: ContactFormat = ContactFormat.PLAIN,
    val photoBase64: String = "",
    val createdAt: Instant = Instant.now()
)

fun Contact.displayLabel(): String = displayName.ifBlank { name }
