package com.example.myemailapp.data.model

import com.google.firebase.firestore.PropertyName

data class EmailMetadataDto(
    val tags: List<TagDto> = emptyList(),
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @get:PropertyName("isStarred") @set:PropertyName("isStarred")
    var isStarred: Boolean = false
)
