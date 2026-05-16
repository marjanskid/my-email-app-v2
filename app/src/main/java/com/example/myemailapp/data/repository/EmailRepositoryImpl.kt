package com.example.myemailapp.data.repository

import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Collections
import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Field
import com.example.myemailapp.data.mapper.toDomain
import com.example.myemailapp.data.mapper.toDto
import com.example.myemailapp.data.model.EmailDocumentDto
import com.example.myemailapp.data.model.EmailMetadataDto
import com.example.myemailapp.domain.exception.EmailException
import com.example.myemailapp.domain.model.Tag
import com.example.myemailapp.domain.model.db.Email
import com.example.myemailapp.domain.model.db.EmailStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class EmailRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : EmailRepository {

    companion object {
        private const val TRASH_FOLDER_ID = "folder-trash"
    }

    private val currentUserEmail: String
        get() = auth.currentUser?.email
            ?: throw EmailException.NotAuthenticated()

    private val currentUserId: String
        get() = auth.currentUser?.uid
            ?: throw EmailException.NotAuthenticated()

    private fun userMessagesCollection() =
        db.collection(Collections.USERS)
            .document(currentUserId)
            .collection(Collections.MESSAGES)

    private fun allMessagesCollection() =
        db.collection(Collections.ALL_MESSAGES)

    private fun userEmailMetadataCollection() =
        db.collection(Collections.USERS)
            .document(currentUserId)
            .collection(Collections.MESSAGES_METADATA)

    override suspend fun send(email: Email): Result<String> =
        saveEmail(email, EmailStatus.sent)

    override suspend fun saveDraft(email: Email): Result<String> =
        saveEmail(email, EmailStatus.draft)

    private suspend fun saveEmail(email: Email, status: EmailStatus): Result<String> {
        return try {
            val emailId = email.id.ifEmpty { UUID.randomUUID().toString() }

            val emailDto = email.copy(
                id = emailId,
                from = currentUserEmail,
                status = status.name
            ).toDto()

            val attachmentDtos = email.attachments.map { it.toDto() }

            // Build recipients array from to, cc, bcc (lowercase for case-insensitive matching)
            val recipients = buildList {
                email.to.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach { add(it) }
                email.cc.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach { add(it) }
                email.bcc.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach { add(it) }
            }.distinct()

            // No tags in the document - tags are stored in user-specific metadata
            val firestoreData = mapOf(
                "email" to emailDto,
                "attachments" to attachmentDtos,
                "recipients" to recipients
            )

            // Write to user's messages collection (backward compatibility)
            userMessagesCollection()
                .document(emailId)
                .set(firestoreData)
                .await()

            // Also write to allMessages collection for querying received emails
            allMessagesCollection()
                .document(emailId)
                .set(firestoreData)
                .await()

            Result.success(emailId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEmailById(emailId: String): Result<Email> {
        return try {
            // First try allMessages collection (for received emails)
            var document = allMessagesCollection()
                .document(emailId)
                .get()
                .await()

            // Fallback to user's messages if not found in allMessages
            if (!document.exists()) {
                document = userMessagesCollection()
                    .document(emailId)
                    .get()
                    .await()
            }

            if (!document.exists()) {
                return Result.failure(EmailException.NotFound(emailId))
            }

            val emailDoc = document.toObject<EmailDocumentDto>()
                ?: return Result.failure(EmailException.InvalidData("Failed to parse document"))

            // Fetch user-specific metadata
            val metadataResult = fetchEmailMetadata(emailId)
            if (metadataResult.isFailure) {
                return Result.failure(metadataResult.exceptionOrNull()
                    ?: Exception("Failed to fetch email metadata"))
            }
            val metadata = metadataResult.getOrNull()

            val email = emailDoc.email.toDomain(
                attachments = emailDoc.attachments.map { it.toDomain() },
                tags = metadata?.tags?.map { it.toDomain() } ?: emptyList(),
                isRead = metadata?.isRead ?: false,
                isStarred = metadata?.isStarred ?: false,
                isDeleted = metadata?.isDeleted ?: false,
                metadataFolderId = metadata?.folderId
            )
            Result.success(email)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEmail(emailId: String): Result<Unit> {
        return try {
            userMessagesCollection()
                .document(emailId)
                .delete()
                .await()

            // Also delete metadata if exists
            userEmailMetadataCollection()
                .document(emailId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReceivedEmailsPaginated(
        pageSize: Int,
        lastDocumentId: String?
    ): Result<PaginatedEmailsResult> {
        return try {
            val userEmailLowercase = currentUserEmail.lowercase()

            // Build the base query
            var query = allMessagesCollection()
                .whereArrayContains(Field.RECIPIENTS, userEmailLowercase)
                .orderBy(Field.EMAIL_DATE_TIME, Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            // Apply cursor if we have a last document ID (for subsequent pages)
            if (lastDocumentId != null) {
                val lastDocSnapshot = allMessagesCollection()
                    .document(lastDocumentId)
                    .get()
                    .await()
                if (lastDocSnapshot.exists()) {
                    query = query.startAfter(lastDocSnapshot)
                }
            }

            val querySnapshot = query.get().await()

            // Fetch all email IDs to batch fetch metadata
            val emailIds = querySnapshot.documents.mapNotNull { it.id }

            // Batch fetch all metadata for these emails
            val metadataResult = fetchEmailMetadataBatch(emailIds)
            if (metadataResult.isFailure) {
                return Result.failure(metadataResult.exceptionOrNull()
                    ?: Exception("Failed to fetch email metadata"))
            }
            val metadataMap = metadataResult.getOrNull() ?: emptyMap()

            val emails = querySnapshot.documents.mapNotNull { document ->
                val emailDoc = document.toObject<EmailDocumentDto>() ?: return@mapNotNull null
                val emailId = document.id
                val metadata = metadataMap[emailId]

                emailDoc.email.toDomain(
                    attachments = emailDoc.attachments.map { it.toDomain() },
                    tags = metadata?.tags?.map { it.toDomain() } ?: emptyList(),
                    isRead = metadata?.isRead ?: false,
                    isStarred = metadata?.isStarred ?: false,
                    isDeleted = metadata?.isDeleted ?: false,
                    metadataFolderId = metadata?.folderId
                )
            }.filter { !it.isDeleted }

            // Get the last document ID for cursor-based pagination
            val newLastDocumentId = querySnapshot.documents.lastOrNull()?.id

            // Determine if there are more emails to load
            val hasMore = querySnapshot.documents.size == pageSize

            Result.success(
                PaginatedEmailsResult(
                    emails = emails,
                    lastDocumentId = newLastDocumentId,
                    hasMore = hasMore
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTag(emailId: String, tag: Tag): Result<Unit> {
        return try {
            val tagDto = tag.toDto()
            val metadataRef = userEmailMetadataCollection().document(emailId)

            // Get current metadata
            val currentMetadata = metadataRef.get().await().toObject<EmailMetadataDto>()

            if (currentMetadata != null) {
                // Check if tag already exists
                val existingTags = currentMetadata.tags
                if (existingTags.none { it.id == tag.id }) {
                    // Add new tag
                    metadataRef.update("tags", FieldValue.arrayUnion(tagDto)).await()
                }
            } else {
                // Create new metadata document with this tag
                val newMetadata = EmailMetadataDto(
                    tags = listOf(tagDto),
                    isRead = false,
                    isStarred = false
                )
                metadataRef.set(newMetadata).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTag(emailId: String, tagId: String): Result<Unit> {
        return try {
            val metadataRef = userEmailMetadataCollection().document(emailId)

            // Get current metadata
            val currentMetadata = metadataRef.get().await().toObject<EmailMetadataDto>()

            if (currentMetadata != null) {
                // Find and remove the tag
                val tagToRemove = currentMetadata.tags.find { it.id == tagId }
                if (tagToRemove != null) {
                    metadataRef.update("tags", FieldValue.arrayRemove(tagToRemove)).await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(emailId: String, isRead: Boolean): Result<Unit> {
        return try {
            val metadataRef = userEmailMetadataCollection().document(emailId)
            val currentMetadata = metadataRef.get().await().toObject<EmailMetadataDto>()

            if (currentMetadata != null) {
                metadataRef.update("isRead", isRead).await()
            } else {
                // Create new metadata document
                val newMetadata = EmailMetadataDto(
                    tags = emptyList(),
                    isRead = isRead,
                    isStarred = false
                )
                metadataRef.set(newMetadata).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleStar(emailId: String, isStarred: Boolean): Result<Unit> {
        return try {
            val metadataRef = userEmailMetadataCollection().document(emailId)
            val currentMetadata = metadataRef.get().await().toObject<EmailMetadataDto>()

            if (currentMetadata != null) {
                metadataRef.update("isStarred", isStarred).await()
            } else {
                // Create new metadata document
                val newMetadata = EmailMetadataDto(
                    tags = emptyList(),
                    isRead = false,
                    isStarred = isStarred
                )
                metadataRef.set(newMetadata).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchEmailMetadata(emailId: String): Result<EmailMetadataDto?> {
        return try {
            val metadata = userEmailMetadataCollection()
                .document(emailId)
                .get()
                .await()
                .toObject<EmailMetadataDto>()
            Result.success(metadata)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchEmailMetadataBatch(emailIds: List<String>): Result<Map<String, EmailMetadataDto>> {
        if (emailIds.isEmpty()) return Result.success(emptyMap())

        return try {
            // Firestore limits whereIn to 30 items, so we need to batch
            val result = mutableMapOf<String, EmailMetadataDto>()

            emailIds.chunked(30).forEach { chunk ->
                val querySnapshot = userEmailMetadataCollection()
                    .whereIn("__name__", chunk.map { userEmailMetadataCollection().document(it) })
                    .get()
                    .await()

                querySnapshot.documents.forEach { doc ->
                    doc.toObject<EmailMetadataDto>()?.let { metadata ->
                        result[doc.id] = metadata
                    }
                }
            }

            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEmailCountByFolderId(folderId: String): Result<Int> {
        return try {
            // Query metadata collection for matching per-user folderId
            val querySnapshot = userEmailMetadataCollection()
                .whereEqualTo("folderId", folderId)
                .get()
                .await()

            Result.success(querySnapshot.size())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEmailsByFolderId(folderId: String): Result<List<Email>> {
        return try {
            // Step 1: Query metadata collection for emails with matching per-user folderId
            val metadataSnapshot = userEmailMetadataCollection()
                .whereEqualTo("folderId", folderId)
                .get()
                .await()

            val emailIds = metadataSnapshot.documents.mapNotNull { it.id }
            if (emailIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Build metadata map from the query results
            val metadataMap = metadataSnapshot.documents.mapNotNull { doc ->
                val metadata = doc.toObject<EmailMetadataDto>()
                if (metadata != null) doc.id to metadata else null
            }.toMap()

            // Step 2: Fetch emails from allMessages collection
            // Firestore limits whereIn to 30 items, so we need to batch
            val emails = mutableListOf<Email>()

            emailIds.chunked(30).forEach { chunk ->
                val querySnapshot = allMessagesCollection()
                    .whereIn("email.id", chunk)
                    .get()
                    .await()

                querySnapshot.documents.mapNotNull { document ->
                    val emailDoc = document.toObject<EmailDocumentDto>() ?: return@mapNotNull null
                    val emailId = document.id
                    val metadata = metadataMap[emailId]

                    emailDoc.email.toDomain(
                        attachments = emailDoc.attachments.map { it.toDomain() },
                        tags = metadata?.tags?.map { it.toDomain() } ?: emptyList(),
                        isRead = metadata?.isRead ?: false,
                        isStarred = metadata?.isStarred ?: false,
                        isDeleted = metadata?.isDeleted ?: false,
                        metadataFolderId = metadata?.folderId
                    )
                }.let { emails.addAll(it) }
            }

            // Sort by dateTime descending
            Result.success(emails.sortedByDescending { it.dateTime })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun softDeleteEmail(emailId: String): Result<Unit> {
        return try {
            val metadataRef = userEmailMetadataCollection().document(emailId)
            val currentMetadata = metadataRef.get().await().toObject<EmailMetadataDto>()

            if (currentMetadata != null) {
                metadataRef.update(
                    mapOf(
                        "isDeleted" to true,
                        "folderId" to TRASH_FOLDER_ID
                    )
                ).await()
            } else {
                // Create new metadata document with soft-delete flags
                val newMetadata = EmailMetadataDto(
                    folderId = TRASH_FOLDER_ID,
                    isDeleted = true
                )
                metadataRef.set(newMetadata).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
