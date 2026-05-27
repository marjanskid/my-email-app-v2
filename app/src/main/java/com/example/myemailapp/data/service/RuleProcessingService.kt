package com.example.myemailapp.data.service

import com.example.myemailapp.data.model.EmailDocumentDto
import com.example.myemailapp.data.model.EmailMetadataDto
import com.example.myemailapp.domain.model.Rule

interface RuleProcessingService {
    fun applyRulesToNewEmail(
        emailDoc: EmailDocumentDto,
        rules: List<Rule>
    ): EmailMetadataDto?
}
