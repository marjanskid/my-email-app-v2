package com.example.myemailapp.domain.model

object PredefinedTags {
    val ALL = listOf(
        Tag(id = "tag-work", name = "Work", color = "#4CAF50"),
        Tag(id = "tag-personal", name = "Personal", color = "#2196F3"),
        Tag(id = "tag-important", name = "Important", color = "#F44336"),
        Tag(id = "tag-urgent", name = "Urgent", color = "#FF9800"),
        Tag(id = "tag-newsletter", name = "Newsletter", color = "#9C27B0"),
        Tag(id = "tag-finance", name = "Finance", color = "#009688")
    )
}
