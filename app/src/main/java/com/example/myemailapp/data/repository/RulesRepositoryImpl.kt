package com.example.myemailapp.data.repository

import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Collections
import com.example.myemailapp.data.mapper.toDomain
import com.example.myemailapp.data.mapper.toDto
import com.example.myemailapp.data.model.RuleDto
import com.example.myemailapp.domain.model.Rule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class RulesRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : RulesRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userRulesCollection() =
        db.collection(Collections.USERS)
            .document(currentUserId)
            .collection(Collections.RULES)

    override suspend fun getRules(): Result<List<Rule>> {
        return try {
            val documents = userRulesCollection()
                .get()
                .await()

            val rules = documents.mapNotNull { doc ->
                doc.toObject<RuleDto>().toDomain()
            }
            Result.success(rules)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createRule(rule: Rule): Result<String> {
        return try {
            val ruleId = rule.id.ifEmpty { UUID.randomUUID().toString() }
            val ruleDto = rule.copy(id = ruleId).toDto()

            userRulesCollection()
                .document(ruleId)
                .set(ruleDto)
                .await()

            Result.success(ruleId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRule(ruleId: String): Result<Unit> {
        return try {
            userRulesCollection()
                .document(ruleId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
