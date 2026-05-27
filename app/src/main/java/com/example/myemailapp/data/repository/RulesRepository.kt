package com.example.myemailapp.data.repository

import com.example.myemailapp.domain.model.Rule

interface RulesRepository {
    suspend fun getRules(): Result<List<Rule>>
    suspend fun createRule(rule: Rule): Result<String>
    suspend fun deleteRule(ruleId: String): Result<Unit>
}
