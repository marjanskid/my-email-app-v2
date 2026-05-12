package com.example.myemailapp.domain.model

import java.time.Instant

data class Folder(
    val id: String = "",
    val name: String = "",
    val parentId: String? = null,
    val createdAt: Instant = Instant.now()
)
