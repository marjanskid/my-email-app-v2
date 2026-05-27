package com.example.myemailapp.data.mapper

import com.example.myemailapp.data.model.AttachmentDto
import com.example.myemailapp.data.model.EmailDto
import com.example.myemailapp.data.model.TagDto
import com.example.myemailapp.domain.model.Attachment
import com.example.myemailapp.domain.model.Tag
import com.example.myemailapp.domain.model.db.Email
import com.google.firebase.Timestamp
import java.time.Instant

fun EmailDto.toDomain(
    attachments: List<Attachment>,
    tags: List<Tag> = emptyList(),
    isRead: Boolean = false,
    isStarred: Boolean = false,
    isDeleted: Boolean = false,
    metadataFolderId: String? = null
): Email = Email(
    id = id,
    from = from,
    to = to,
    cc = cc,
    bcc = bcc,
    subject = subject,
    content = content,
    attachments = attachments,
    dateTime = dateTime?.toDate()?.toInstant() ?: Instant.now(),
    status = status,
    folderId = metadataFolderId ?: folderId,
    tags = tags,
    isRead = isRead,
    isStarred = isStarred,
    isDeleted = isDeleted
)

fun Email.toDto(): EmailDto = EmailDto(
    id = id,
    from = from,
    to = to,
    cc = cc,
    bcc = bcc,
    subject = subject,
    content = content,
    dateTime = Timestamp(java.util.Date.from(dateTime)),
    status = status,
    folderId = folderId
)

fun AttachmentDto.toDomain(): Attachment = Attachment(
    id = id,
    dataBase64 = dataBase64,
    type = type,
    name = name
)

fun Attachment.toDto(): AttachmentDto = AttachmentDto(
    id = id,
    dataBase64 = dataBase64,
    type = type,
    name = name
)

fun TagDto.toDomain(): Tag = Tag(
    id = id,
    name = name,
    color = color
)

fun Tag.toDto(): TagDto = TagDto(
    id = id,
    name = name,
    color = color
)
