package com.example.myemailapp.data.mapper

import com.example.myemailapp.data.model.ContactDto
import com.example.myemailapp.domain.model.Contact
import com.example.myemailapp.domain.model.ContactFormat
import com.google.firebase.Timestamp
import java.time.Instant
import java.util.Date

fun ContactDto.toDomain(): Contact = Contact(
    id = id,
    name = name,
    displayName = displayName,
    email = email,
    format = runCatching { ContactFormat.valueOf(format) }.getOrDefault(ContactFormat.PLAIN),
    photoBase64 = photoBase64,
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now()
)

fun Contact.toDto(): ContactDto = ContactDto(
    id = id,
    name = name,
    displayName = displayName,
    email = email,
    format = format.name,
    photoBase64 = photoBase64,
    createdAt = Timestamp(Date.from(createdAt))
)
