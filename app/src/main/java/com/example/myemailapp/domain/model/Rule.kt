package com.example.myemailapp.domain.model

data class Rule(
    val id: String = "",
    val condition: Condition,
    val conditionValue: String,
    val operation: Operation,
    val destinationFolderId: String,
    val destinationFolderName: String
)

enum class Condition {
    TO,
    FROM,
    CC,
    SUBJECT
}

enum class Operation {
    MOVE,
    COPY,
    DELETE
}
