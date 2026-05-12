package com.example.myemailapp.data.mapper

import com.example.myemailapp.data.model.ContactDto
import com.example.myemailapp.domain.model.Contact
import com.google.firebase.Timestamp
import java.time.Instant

fun ContactDto.toDomain(): Contact = Contact(
    id = id,
    name = name,
    email = email,
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now()
)

fun Contact.toDto(): ContactDto = ContactDto(
    id = id,
    name = name,
    email = email,
    createdAt = Timestamp(java.util.Date.from(createdAt))
)
