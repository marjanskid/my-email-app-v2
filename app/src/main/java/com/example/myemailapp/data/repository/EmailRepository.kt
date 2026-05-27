package com.example.myemailapp.data.repository

import com.example.myemailapp.domain.model.Tag
import com.example.myemailapp.domain.model.db.Email

/**
 * Result of a paginated email query.
 */
data class PaginatedEmailsResult(
    val emails: List<Email>,
    val lastDocumentId: String?,
    val hasMore: Boolean
)

interface EmailRepository {

    companion object {
        /** Default page size for email pagination */
        const val DEFAULT_PAGE_SIZE = 10
    }

    suspend fun send(email: Email): Result<String>
    suspend fun saveDraft(email: Email): Result<String>
    suspend fun getEmailById(emailId: String): Result<Email>
    suspend fun deleteEmail(emailId: String): Result<Unit>

    /**
     * Gets received emails with pagination support.
     * @param pageSize Number of emails to fetch per page (default: 10)
     * @param lastDocumentId The ID of the last document from the previous page for cursor-based pagination.
     *                       Pass null to get the first page.
     * @return Paginated result containing emails, the last document ID for next page, and hasMore flag.
     */
    suspend fun getReceivedEmailsPaginated(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastDocumentId: String? = null
    ): Result<PaginatedEmailsResult>

    suspend fun addTag(emailId: String, tag: Tag): Result<Unit>
    suspend fun removeTag(emailId: String, tagId: String): Result<Unit>
    suspend fun markAsRead(emailId: String, isRead: Boolean): Result<Unit>
    suspend fun toggleStar(emailId: String, isStarred: Boolean): Result<Unit>
    suspend fun getEmailCountByFolderId(folderId: String): Result<Int>
    suspend fun getEmailsByFolderId(folderId: String): Result<List<Email>>
    suspend fun softDeleteEmail(emailId: String): Result<Unit>
}