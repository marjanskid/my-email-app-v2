package com.example.myemailapp.data.mapper

import com.example.myemailapp.data.model.RuleDto
import com.example.myemailapp.domain.model.Condition
import com.example.myemailapp.domain.model.Operation
import com.example.myemailapp.domain.model.Rule

fun RuleDto.toDomain(): Rule = Rule(
    id = id,
    condition = Condition.valueOf(condition.ifEmpty { "FROM" }),
    conditionValue = conditionValue,
    operation = Operation.valueOf(operation.ifEmpty { "MOVE" }),
    destinationFolderId = destinationFolderId,
    destinationFolderName = destinationFolderName
)

fun Rule.toDto(): RuleDto = RuleDto(
    id = id,
    condition = condition.name,
    conditionValue = conditionValue,
    operation = operation.name,
    destinationFolderId = destinationFolderId,
    destinationFolderName = destinationFolderName
)
