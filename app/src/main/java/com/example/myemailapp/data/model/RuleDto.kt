package com.example.myemailapp.data.model

data class RuleDto(
    val id: String = "",
    val condition: String = "",
    val conditionValue: String = "",
    val operation: String = "",
    val destinationFolderId: String = "",
    val destinationFolderName: String = ""
)
