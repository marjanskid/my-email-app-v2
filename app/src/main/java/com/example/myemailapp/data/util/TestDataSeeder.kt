package com.example.myemailapp.data.util

import android.util.Log
import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Collections
import com.example.myemailapp.data.model.AttachmentDto
import com.example.myemailapp.data.model.EmailDto
import com.example.myemailapp.data.model.EmailMetadataDto
import com.example.myemailapp.data.model.RuleDto
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException

class TestDataSeeder(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "TestDataSeeder"
    }

    // Test users with hardcoded Firebase Auth UIDs
    private object TestUsers {
        const val DUSAN_EMAIL = "dmarjanski@email.app"
        const val DUSAN_UID = "05uS8DRYj5ZAPEJrpkG1rXdccmz2"
        const val MARKO_EMAIL = "mmarkovic@email.app"
        const val MARKO_UID = "0wfIFsTxqGgx9t8CMdeRtmckt1k1"
        const val PETAR_EMAIL = "ppetrovic@email.app"
        const val PETAR_UID = "DW77lMBOtlgKc2wgRo1Aa9pTn8z1"

        val ALL_EMAILS = listOf(DUSAN_EMAIL, MARKO_EMAIL, PETAR_EMAIL)
        val ALL_UIDS = listOf(DUSAN_UID, MARKO_UID, PETAR_UID)
    }

    // Folder IDs
    private object Folders {
        const val INBOX = "folder-inbox"
        const val SENT = "folder-sent"
        const val WORK = "folder-work"
        const val PERSONAL = "folder-personal"
        const val SPAM = "folder-spam"
        const val TRASH = "folder-trash"
    }

    // Folder definitions (6 folders per user)
    private data class FolderDef(val id: String, val name: String)

    private val folders = listOf(
        FolderDef(Folders.INBOX, "Inbox"),
        FolderDef(Folders.SENT, "Sent"),
        FolderDef(Folders.WORK, "Work"),
        FolderDef(Folders.PERSONAL, "Personal"),
        FolderDef(Folders.SPAM, "Spam"),
        FolderDef(Folders.TRASH, "Trash")
    )

    // Rule definition per user
    private data class UserRule(
        val userId: String,
        val ruleId: String,
        val condition: String,
        val conditionValue: String,
        val operation: String,
        val destinationFolderId: String,
        val destinationFolderName: String
    )

    private val userRules = listOf(
        // Dusan: FROM contains "work" → MOVE to Work folder
        UserRule(
            userId = TestUsers.DUSAN_UID,
            ruleId = "rule-dusan-work",
            condition = "FROM",
            conditionValue = "work",
            operation = "MOVE",
            destinationFolderId = Folders.WORK,
            destinationFolderName = "Work"
        ),
        // Marko: SUBJECT contains "urgent" → MOVE to Personal folder
        UserRule(
            userId = TestUsers.MARKO_UID,
            ruleId = "rule-marko-urgent",
            condition = "SUBJECT",
            conditionValue = "urgent",
            operation = "MOVE",
            destinationFolderId = Folders.PERSONAL,
            destinationFolderName = "Personal"
        ),
        // Petar: FROM contains "spam" → DELETE (move to Trash)
        UserRule(
            userId = TestUsers.PETAR_UID,
            ruleId = "rule-petar-spam",
            condition = "FROM",
            conditionValue = "spam",
            operation = "DELETE",
            destinationFolderId = Folders.TRASH,
            destinationFolderName = "Trash"
        )
    )

    // Email definition with expected folder per user
    private data class TestEmail(
        val id: String,
        val from: String,
        val subject: String,
        val content: String,
        val hoursAgo: Long,
        // Which folder each user should see this email in (based on their rules)
        val dusanFolder: String,
        val markoFolder: String,
        val petarFolder: String
    )

    private val testEmails = listOf(
        // test-001: Normal email - goes to Inbox for everyone
        TestEmail(
            id = "test-001",
            from = "friend@gmail.com",
            subject = "Hello there",
            content = "Hey! Just wanted to say hello and see how you're doing. Hope everything is going well!",
            hoursAgo = 4,
            dusanFolder = Folders.INBOX,
            markoFolder = Folders.INBOX,
            petarFolder = Folders.INBOX
        ),
        // test-002: FROM contains "work" - triggers Dusan's rule (→ Work folder)
        TestEmail(
            id = "test-002",
            from = "work@company.com",
            subject = "Project update",
            content = "Hi team, here's the latest update on the project. All milestones are on track for delivery.",
            hoursAgo = 3,
            dusanFolder = Folders.WORK,
            markoFolder = Folders.INBOX,
            petarFolder = Folders.INBOX
        ),
        // test-003: SUBJECT contains "URGENT" - triggers Marko's rule (→ Personal folder)
        TestEmail(
            id = "test-003",
            from = "boss@email.com",
            subject = "URGENT: Review needed",
            content = "Please review the attached document and provide your feedback by end of day. This is urgent.",
            hoursAgo = 2,
            dusanFolder = Folders.INBOX,
            markoFolder = Folders.PERSONAL,
            petarFolder = Folders.INBOX
        ),
        // test-004: FROM contains "spam" - triggers Petar's rule (→ Trash)
        TestEmail(
            id = "test-004",
            from = "spam@ads.com",
            subject = "Buy now!",
            content = "Amazing deals just for you! Click here to claim your prize and save big money today!",
            hoursAgo = 1,
            dusanFolder = Folders.INBOX,
            markoFolder = Folders.INBOX,
            petarFolder = Folders.TRASH
        )
    )

    /**
     * Seeds all test data:
     * - 6 Folders per user (Inbox, Sent, Work, Personal, Spam, Trash)
     * - 1 Rule per user (Dusan: work→Work, Marko: urgent→Personal, Petar: spam→Trash)
     * - 4 Emails with metadata placed according to each user's rules
     */
    suspend fun seedTestData(): Result<Unit> {
        auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        return try {
            Log.d(TAG, "Starting seedTestData")

            // Step 1: Create folders for each user
            seedFolders()

            // Step 2: Create rules for each user
            seedRules()

            // Step 3: Create emails in allMessages collection
            seedEmails()

            // Step 4: Create metadata for each user with correct folder placement
            seedMetadata()

            Log.d(TAG, "seedTestData completed successfully")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed test data: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun seedFolders() {
        TestUsers.ALL_UIDS.forEachIndexed { index, userId ->
            val userEmail = TestUsers.ALL_EMAILS[index]
            val foldersCollection = firestore.collection(Collections.USERS)
                .document(userId)
                .collection(Collections.FOLDERS)

            folders.forEach { folder ->
                val folderData = mapOf(
                    "id" to folder.id,
                    "name" to folder.name
                )
                foldersCollection.document(folder.id).set(folderData).await()
            }
            Log.d(TAG, "Created ${folders.size} folders for $userEmail")
        }
    }

    private suspend fun seedRules() {
        userRules.forEach { rule ->
            val rulesCollection = firestore.collection(Collections.USERS)
                .document(rule.userId)
                .collection(Collections.RULES)

            val ruleDto = RuleDto(
                id = rule.ruleId,
                condition = rule.condition,
                conditionValue = rule.conditionValue,
                operation = rule.operation,
                destinationFolderId = rule.destinationFolderId,
                destinationFolderName = rule.destinationFolderName
            )
            rulesCollection.document(rule.ruleId).set(ruleDto).await()
            Log.d(TAG, "Created rule ${rule.ruleId} for user ${rule.userId}")
        }
    }

    private suspend fun seedEmails() {
        val allMessagesCollection = firestore.collection(Collections.ALL_MESSAGES)
        val toField = TestUsers.ALL_EMAILS.joinToString(", ")

        testEmails.forEach { email ->
            val dateTime = Timestamp(Date(System.currentTimeMillis() - email.hoursAgo * 60 * 60 * 1000L))
            val recipients = buildRecipients(email.from, TestUsers.ALL_EMAILS)

            val emailData = mapOf(
                "email" to EmailDto(
                    id = email.id,
                    from = email.from,
                    to = toField,
                    cc = "",
                    bcc = "",
                    subject = email.subject,
                    content = email.content,
                    dateTime = dateTime,
                    status = "sent",
                    folderId = null
                ),
                "attachments" to emptyList<AttachmentDto>(),
                "recipients" to recipients
            )

            allMessagesCollection.document(email.id).set(emailData).await()
            Log.d(TAG, "Created email ${email.id}: ${email.subject}")
        }
    }

    private suspend fun seedMetadata() {
        testEmails.forEach { email ->
            // Dusan's metadata
            createMetadataForUser(TestUsers.DUSAN_UID, email.id, email.dusanFolder)
            // Marko's metadata
            createMetadataForUser(TestUsers.MARKO_UID, email.id, email.markoFolder)
            // Petar's metadata
            createMetadataForUser(TestUsers.PETAR_UID, email.id, email.petarFolder)
        }
    }

    private suspend fun createMetadataForUser(userId: String, emailId: String, folderId: String) {
        val metadataCollection = firestore.collection(Collections.USERS)
            .document(userId)
            .collection(Collections.MESSAGES_METADATA)

        val isDeleted = folderId == Folders.TRASH
        val metadataDto = EmailMetadataDto(
            tags = emptyList(),
            isRead = false,
            isStarred = false,
            folderId = folderId,
            isDeleted = isDeleted
        )
        metadataCollection.document(emailId).set(metadataDto).await()
        Log.d(TAG, "Created metadata for email $emailId, user $userId, folder $folderId")
    }

    private fun buildRecipients(from: String, toEmails: List<String>): List<String> {
        return buildList {
            from.trim().lowercase().takeIf { it.isNotEmpty() }?.let { add(it) }
            toEmails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach { add(it) }
        }.distinct()
    }

    /**
     * Clears ALL data from Firestore:
     * - allMessages (all documents)
     * - users/{uid}/messagesMetadata (for all 3 users)
     * - users/{uid}/folders (for all 3 users)
     * - users/{uid}/rules (for all 3 users)
     */
    suspend fun clearAllData(): Result<Unit> {
        auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        return try {
            Log.d(TAG, "Starting clearAllData")

            // Delete all test emails from allMessages
            val allMessagesCollection = firestore.collection(Collections.ALL_MESSAGES)
            testEmails.forEach { email ->
                try {
                    allMessagesCollection.document(email.id).delete().await()
                    Log.d(TAG, "Deleted email: ${email.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not delete ${email.id}: ${e.message}")
                }
            }

            // Delete metadata, folders, and rules for all 3 users
            TestUsers.ALL_UIDS.forEachIndexed { index, userId ->
                val userEmail = TestUsers.ALL_EMAILS[index]

                // Delete metadata
                val metadataCollection = firestore.collection(Collections.USERS)
                    .document(userId)
                    .collection(Collections.MESSAGES_METADATA)
                testEmails.forEach { email ->
                    try {
                        metadataCollection.document(email.id).delete().await()
                    } catch (_: Exception) { }
                }
                Log.d(TAG, "Deleted metadata for $userEmail")

                // Delete folders
                val foldersCollection = firestore.collection(Collections.USERS)
                    .document(userId)
                    .collection(Collections.FOLDERS)
                folders.forEach { folder ->
                    try {
                        foldersCollection.document(folder.id).delete().await()
                    } catch (_: Exception) { }
                }
                Log.d(TAG, "Deleted folders for $userEmail")

                // Delete rules
                val rulesCollection = firestore.collection(Collections.USERS)
                    .document(userId)
                    .collection(Collections.RULES)
                userRules.filter { it.userId == userId }.forEach { rule ->
                    try {
                        rulesCollection.document(rule.ruleId).delete().await()
                    } catch (_: Exception) { }
                }
                Log.d(TAG, "Deleted rules for $userEmail")
            }

            // Clean up legacy test data from old seeder
            cleanupLegacyData()

            Log.d(TAG, "clearAllData completed successfully")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear data: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun cleanupLegacyData() {
        val allMessagesCollection = firestore.collection(Collections.ALL_MESSAGES)

        // Legacy email IDs from previous seeder versions
        val legacyEmailIds = buildList {
            // Old test-init-* and test-new-* patterns
            (1..5).forEach { add("test-init-${it.toString().padStart(3, '0')}") }
            (1..4).forEach { add("test-new-${it.toString().padStart(3, '0')}") }
            // Very old patterns
            (1..39).forEach { add("test-${it.toString().padStart(3, '0')}") }
            addAll(listOf("email-001", "email-002", "email-003", "email-004", "email-005", "email-006"))
        }

        legacyEmailIds.forEach { emailId ->
            try {
                allMessagesCollection.document(emailId).delete().await()
            } catch (_: Exception) { }
        }

        // Legacy folder IDs
        val legacyFolderIds = listOf("folder-promotions")

        // Legacy rule IDs
        val legacyRuleIds = listOf("rule-work", "rule-spam")

        // Old user UIDs (from previous seeder)
        val oldUserUids = listOf(
            "TDuLwVlceIefe6Sbafr8GtcABRP2",
            "sH5evcNw3ORTtWuWSvaLYpijyAJ3",
            "tg7qIKiopWU0C4H0tDq9dSXCCbr1"
        )

        // Clean up for all users (old and new)
        val allUserUids = (TestUsers.ALL_UIDS + oldUserUids).distinct()

        allUserUids.forEach { userId ->
            try {
                // Delete legacy metadata
                val metadataCollection = firestore.collection(Collections.USERS)
                    .document(userId)
                    .collection(Collections.MESSAGES_METADATA)
                legacyEmailIds.forEach { emailId ->
                    try {
                        metadataCollection.document(emailId).delete().await()
                    } catch (_: Exception) { }
                }

                // Delete legacy folders
                val foldersCollection = firestore.collection(Collections.USERS)
                    .document(userId)
                    .collection(Collections.FOLDERS)
                (folders.map { it.id } + legacyFolderIds).forEach { folderId ->
                    try {
                        foldersCollection.document(folderId).delete().await()
                    } catch (_: Exception) { }
                }

                // Delete legacy rules
                val rulesCollection = firestore.collection(Collections.USERS)
                    .document(userId)
                    .collection(Collections.RULES)
                legacyRuleIds.forEach { ruleId ->
                    try {
                        rulesCollection.document(ruleId).delete().await()
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }

        Log.d(TAG, "Cleaned up legacy test data")
    }
}
