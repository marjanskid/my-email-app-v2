package com.example.myemailapp.data.mapper

import com.example.myemailapp.data.model.FolderDto
import com.example.myemailapp.domain.model.Folder
import com.google.firebase.Timestamp
import java.time.Instant

fun FolderDto.toDomain(): Folder = Folder(
    id = id,
    name = name,
    parentId = parentId,
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now()
)

fun Folder.toDto(): FolderDto = FolderDto(
    id = id,
    name = name,
    parentId = parentId,
    createdAt = Timestamp(java.util.Date.from(createdAt))
)
