package com.example.myemailapp.data.service

import com.example.myemailapp.data.model.EmailDocumentDto
import com.example.myemailapp.data.model.EmailMetadataDto
import com.example.myemailapp.domain.model.Condition
import com.example.myemailapp.domain.model.Operation
import com.example.myemailapp.domain.model.Rule

class RuleProcessingServiceImpl : RuleProcessingService {

    companion object {
        private const val TRASH_FOLDER_ID = "folder-trash"
    }

    override fun applyRulesToNewEmail(
        emailDoc: EmailDocumentDto,
        rules: List<Rule>
    ): EmailMetadataDto? {
        if (rules.isEmpty()) return null

        // Find first matching rule (first rule wins)
        val matchingRule = rules.firstOrNull { rule ->
            matchesRule(emailDoc, rule)
        }

        return matchingRule?.let { rule ->
            createMetadataForRule(rule)
        }
    }

    private fun matchesRule(emailDoc: EmailDocumentDto, rule: Rule): Boolean {
        val conditionValue = rule.conditionValue.lowercase().trim()
        if (conditionValue.isEmpty()) return false

        return when (rule.condition) {
            Condition.FROM -> {
                emailDoc.email.from.lowercase().contains(conditionValue)
            }
            Condition.TO -> {
                emailDoc.email.to.lowercase().contains(conditionValue)
            }
            Condition.CC -> {
                emailDoc.email.cc.lowercase().contains(conditionValue)
            }
            Condition.SUBJECT -> {
                emailDoc.email.subject.lowercase().contains(conditionValue)
            }
        }
    }

    private fun createMetadataForRule(rule: Rule): EmailMetadataDto {
        return when (rule.operation) {
            Operation.DELETE -> EmailMetadataDto(
                isDeleted = true,
                folderId = TRASH_FOLDER_ID
            )
            Operation.MOVE -> EmailMetadataDto(
                folderId = rule.destinationFolderId
            )
            Operation.COPY -> EmailMetadataDto(
                folderId = rule.destinationFolderId
            )
        }
    }
}
