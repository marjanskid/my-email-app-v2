package com.example.myemailapp.data.util

import android.util.Log
import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Collections
import com.example.myemailapp.data.model.AttachmentDto
import com.example.myemailapp.data.model.EmailDto
import com.example.myemailapp.data.model.EmailMetadataDto
import com.example.myemailapp.data.model.TagDto
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

    // Test user email constants
    private object TestUsers {
        const val MILOS = "milos.krstic@myapp.com"
        const val DUSAN = "dusan.m@myemailapp.com"
        const val ARAMBA = "aramba.test@myapp.com"
    }

    // Predefined tags
    private object Tags {
        val WORK = TagDto("tag-work", "Work", "#4CAF50")
        val PERSONAL = TagDto("tag-personal", "Personal", "#2196F3")
        val IMPORTANT = TagDto("tag-important", "Important", "#F44336")
        val URGENT = TagDto("tag-urgent", "Urgent", "#FF9800")
        val NEWSLETTER = TagDto("tag-newsletter", "Newsletter", "#9C27B0")
        val FINANCE = TagDto("tag-finance", "Finance", "#009688")
    }

    // Helper data class for test email definitions
    private data class TestEmail(
        val id: String,
        val from: String,
        val to: String,
        val cc: String = "",
        val bcc: String = "",
        val subject: String,
        val content: String,
        val hoursAgo: Long = 1
    )

    // Helper data class for metadata definitions
    private data class TestMetadata(
        val emailId: String,
        val isRead: Boolean,
        val isStarred: Boolean,
        val tags: List<TagDto> = emptyList()
    )

    private fun buildRecipients(from: String, to: String, cc: String, bcc: String = ""): List<String> {
        return buildList {
            // Include sender so they can see sent emails
            from.trim().lowercase().takeIf { it.isNotEmpty() }?.let { add(it) }
            to.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach { add(it) }
            cc.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach { add(it) }
            bcc.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach { add(it) }
        }.distinct()
    }

    /**
     * Query Firestore to find the user ID for a given email address.
     */
    private suspend fun getUserIdByEmail(email: String): String? {
        return try {
            firestore.collection(Collections.USERS)
                .whereEqualTo("email", email)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user ID for email $email: ${e.message}")
            null
        }
    }

    /**
     * Seeds ALL test data (39 emails + metadata for all 3 users) in one click.
     * Can be run by any authenticated user.
     */
    suspend fun seedAllTestData(): Result<Unit> {
        auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        return try {
            Log.d(TAG, "Starting seedAllTestData - creating 39 test emails for all users")

            // Step 1: Find user IDs for all 3 test users
            val userIds = mapOf(
                TestUsers.MILOS to getUserIdByEmail(TestUsers.MILOS),
                TestUsers.DUSAN to getUserIdByEmail(TestUsers.DUSAN),
                TestUsers.ARAMBA to getUserIdByEmail(TestUsers.ARAMBA)
            )

            Log.d(TAG, "Found user IDs: ${userIds.entries.joinToString { "${it.key} -> ${it.value}" }}")

            // Step 2: Create all 39 test emails in allMessages collection
            val allMessagesCollection = firestore.collection(Collections.ALL_MESSAGES)

            testEmails.forEachIndexed { index, testEmail ->
                try {
                    val dateTime = Timestamp(Date(System.currentTimeMillis() - testEmail.hoursAgo * 60 * 60 * 1000L))
                    val recipients = buildRecipients(testEmail.from, testEmail.to, testEmail.cc, testEmail.bcc)

                    val emailData = mapOf(
                        "email" to EmailDto(
                            id = testEmail.id,
                            from = testEmail.from,
                            to = testEmail.to,
                            cc = testEmail.cc,
                            bcc = testEmail.bcc,
                            subject = testEmail.subject,
                            content = testEmail.content,
                            dateTime = dateTime,
                            status = "sent",
                            folderId = null
                        ),
                        "attachments" to emptyList<AttachmentDto>(),
                        "recipients" to recipients
                    )

                    allMessagesCollection.document(testEmail.id).set(emailData).await()
                    Log.d(TAG, "Email ${index + 1} (${testEmail.id}: ${testEmail.subject}) created successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create email ${index + 1} (${testEmail.id}): ${e.message}", e)
                    throw e
                }
            }

            Log.d(TAG, "All ${testEmails.size} test emails created successfully")

            // Step 3: Create metadata for each found user
            userIds.forEach { (email, userId) ->
                if (userId != null) {
                    try {
                        val metadataList = getMetadataForUser(email)
                        val metadataCollection = firestore.collection(Collections.USERS)
                            .document(userId)
                            .collection(Collections.MESSAGES_METADATA)

                        metadataList.forEach { metadata ->
                            val metadataDto = EmailMetadataDto(
                                tags = metadata.tags,
                                isRead = metadata.isRead,
                                isStarred = metadata.isStarred
                            )
                            metadataCollection.document(metadata.emailId).set(metadataDto).await()
                        }

                        Log.d(TAG, "Created ${metadataList.size} metadata entries for $email (userId: $userId)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create metadata for $email: ${e.message}", e)
                        throw e
                    }
                } else {
                    Log.w(TAG, "User not found for email: $email - skipping metadata creation")
                }
            }

            Log.d(TAG, "seedAllTestData completed successfully")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed all test data: ${e.message}", e)
            Result.failure(e)
        }
    }

    // All 39 test emails with comprehensive TO/CC/BCC combinations
    private val testEmails = listOf(
        // ============================================================================
        // Group A: TO Only - Single Recipient (3 emails) - 2 users per email
        // Tests: User NOT in email should NOT see it
        // ============================================================================
        TestEmail(
            id = "test-001",
            from = TestUsers.MILOS,
            to = TestUsers.DUSAN,
            subject = "[TO:1] M→D",
            content = "Test email from Milos to Dusan only.\n\nThis email should only be visible to Milos and Dusan.\nAramba should NOT see this email.",
            hoursAgo = 1
        ),
        TestEmail(
            id = "test-002",
            from = TestUsers.DUSAN,
            to = TestUsers.ARAMBA,
            subject = "[TO:1] D→A",
            content = "Test email from Dusan to Aramba only.\n\nThis email should only be visible to Dusan and Aramba.\nMilos should NOT see this email.",
            hoursAgo = 2
        ),
        TestEmail(
            id = "test-003",
            from = TestUsers.ARAMBA,
            to = TestUsers.MILOS,
            subject = "[TO:1] A→M",
            content = "Test email from Aramba to Milos only.\n\nThis email should only be visible to Aramba and Milos.\nDusan should NOT see this email.",
            hoursAgo = 3
        ),

        // ============================================================================
        // Group B: TO Only - Two Recipients (3 emails) - 3 users per email
        // ============================================================================
        TestEmail(
            id = "test-004",
            from = TestUsers.MILOS,
            to = "${TestUsers.DUSAN}, ${TestUsers.ARAMBA}",
            subject = "[TO:2] M→D,A",
            content = "Test email from Milos to both Dusan and Aramba.\n\nAll three users should see this email.",
            hoursAgo = 4
        ),
        TestEmail(
            id = "test-005",
            from = TestUsers.DUSAN,
            to = "${TestUsers.MILOS}, ${TestUsers.ARAMBA}",
            subject = "[TO:2] D→M,A",
            content = "Test email from Dusan to both Milos and Aramba.\n\nAll three users should see this email.",
            hoursAgo = 5
        ),
        TestEmail(
            id = "test-006",
            from = TestUsers.ARAMBA,
            to = "${TestUsers.MILOS}, ${TestUsers.DUSAN}",
            subject = "[TO:2] A→M,D",
            content = "Test email from Aramba to both Milos and Dusan.\n\nAll three users should see this email.",
            hoursAgo = 6
        ),

        // ============================================================================
        // Group C: TO Only - 2 users private (6 emails) - 2 users per email
        // Tests: User NOT in TO should NOT see it
        // ============================================================================
        TestEmail(
            id = "test-007",
            from = TestUsers.MILOS,
            to = TestUsers.DUSAN,
            subject = "[TO:1] M→D private",
            content = "Private conversation between Milos and Dusan.\n\nAramba should NOT see this.",
            hoursAgo = 7
        ),
        TestEmail(
            id = "test-008",
            from = TestUsers.DUSAN,
            to = TestUsers.MILOS,
            subject = "[TO:1] D→M private",
            content = "Private conversation between Dusan and Milos.\n\nAramba should NOT see this.",
            hoursAgo = 8
        ),
        TestEmail(
            id = "test-009",
            from = TestUsers.MILOS,
            to = TestUsers.ARAMBA,
            subject = "[TO:1] M→A private",
            content = "Private conversation between Milos and Aramba.\n\nDusan should NOT see this.",
            hoursAgo = 9
        ),
        TestEmail(
            id = "test-010",
            from = TestUsers.ARAMBA,
            to = TestUsers.MILOS,
            subject = "[TO:1] A→M private",
            content = "Private conversation between Aramba and Milos.\n\nDusan should NOT see this.",
            hoursAgo = 10
        ),
        TestEmail(
            id = "test-011",
            from = TestUsers.DUSAN,
            to = TestUsers.ARAMBA,
            subject = "[TO:1] D→A private",
            content = "Private conversation between Dusan and Aramba.\n\nMilos should NOT see this.",
            hoursAgo = 11
        ),
        TestEmail(
            id = "test-012",
            from = TestUsers.ARAMBA,
            to = TestUsers.DUSAN,
            subject = "[TO:1] A→D private",
            content = "Private conversation between Aramba and Dusan.\n\nMilos should NOT see this.",
            hoursAgo = 12
        ),

        // ============================================================================
        // Group D: TO(1) + CC(1) - 3 users (6 emails) - 3 users per email
        // ============================================================================
        TestEmail(
            id = "test-013",
            from = TestUsers.MILOS,
            to = TestUsers.DUSAN,
            cc = TestUsers.ARAMBA,
            subject = "[TO:1,CC:1] M→D cc:A",
            content = "Email from Milos to Dusan, with Aramba CC'd.\n\nAll three users should see this.",
            hoursAgo = 13
        ),
        TestEmail(
            id = "test-014",
            from = TestUsers.MILOS,
            to = TestUsers.ARAMBA,
            cc = TestUsers.DUSAN,
            subject = "[TO:1,CC:1] M→A cc:D",
            content = "Email from Milos to Aramba, with Dusan CC'd.\n\nAll three users should see this.",
            hoursAgo = 14
        ),
        TestEmail(
            id = "test-015",
            from = TestUsers.DUSAN,
            to = TestUsers.MILOS,
            cc = TestUsers.ARAMBA,
            subject = "[TO:1,CC:1] D→M cc:A",
            content = "Email from Dusan to Milos, with Aramba CC'd.\n\nAll three users should see this.",
            hoursAgo = 15
        ),
        TestEmail(
            id = "test-016",
            from = TestUsers.DUSAN,
            to = TestUsers.ARAMBA,
            cc = TestUsers.MILOS,
            subject = "[TO:1,CC:1] D→A cc:M",
            content = "Email from Dusan to Aramba, with Milos CC'd.\n\nAll three users should see this.",
            hoursAgo = 16
        ),
        TestEmail(
            id = "test-017",
            from = TestUsers.ARAMBA,
            to = TestUsers.MILOS,
            cc = TestUsers.DUSAN,
            subject = "[TO:1,CC:1] A→M cc:D",
            content = "Email from Aramba to Milos, with Dusan CC'd.\n\nAll three users should see this.",
            hoursAgo = 17
        ),
        TestEmail(
            id = "test-018",
            from = TestUsers.ARAMBA,
            to = TestUsers.DUSAN,
            cc = TestUsers.MILOS,
            subject = "[TO:1,CC:1] A→D cc:M",
            content = "Email from Aramba to Dusan, with Milos CC'd.\n\nAll three users should see this.",
            hoursAgo = 18
        ),

        // ============================================================================
        // Group E: Mixed TO with external CC (3 emails) - 3 test users see these
        // Natural conversations with external recipients
        // ============================================================================
        TestEmail(
            id = "test-019",
            from = TestUsers.MILOS,
            to = "${TestUsers.DUSAN}, alice@example.com",
            cc = TestUsers.ARAMBA,
            subject = "[TO:2,CC:1] M→D,Alice cc:A",
            content = "Email from Milos to Dusan and Alice, with Aramba CC'd.\n\nAll three test users should see this.",
            hoursAgo = 19
        ),
        TestEmail(
            id = "test-020",
            from = TestUsers.DUSAN,
            to = "${TestUsers.MILOS}, bob@example.com",
            cc = TestUsers.ARAMBA,
            subject = "[TO:2,CC:1] D→M,Bob cc:A",
            content = "Email from Dusan to Milos and Bob, with Aramba CC'd.\n\nAll three test users should see this.",
            hoursAgo = 20
        ),
        TestEmail(
            id = "test-021",
            from = TestUsers.ARAMBA,
            to = "${TestUsers.MILOS}, carol@example.com",
            cc = TestUsers.DUSAN,
            subject = "[TO:2,CC:1] A→M,Carol cc:D",
            content = "Email from Aramba to Milos and Carol, with Dusan CC'd.\n\nAll three test users should see this.",
            hoursAgo = 21
        ),

        // ============================================================================
        // Group F: TO(1) + BCC(1) - BCC with test user (3 emails)
        // Tests: BCC recipient (test user) sees email, but TO recipient doesn't know about BCC
        // External users in TO, test users in BCC
        // ============================================================================
        TestEmail(
            id = "test-022",
            from = TestUsers.MILOS,
            to = "alice@example.com",
            bcc = TestUsers.DUSAN,
            subject = "[TO:1,BCC:1] M→Alice bcc:D",
            content = "Email from Milos to Alice, with Dusan as BCC.\n\nDusan can see this via BCC. Alice is the visible recipient.",
            hoursAgo = 22
        ),
        TestEmail(
            id = "test-023",
            from = TestUsers.DUSAN,
            to = "bob@example.com",
            bcc = TestUsers.ARAMBA,
            subject = "[TO:1,BCC:1] D→Bob bcc:A",
            content = "Email from Dusan to Bob, with Aramba as BCC.\n\nAramba can see this via BCC. Bob is the visible recipient.",
            hoursAgo = 23
        ),
        TestEmail(
            id = "test-024",
            from = TestUsers.ARAMBA,
            to = "carol@example.com",
            bcc = TestUsers.MILOS,
            subject = "[TO:1,BCC:1] A→Carol bcc:M",
            content = "Email from Aramba to Carol, with Milos as BCC.\n\nMilos can see this via BCC. Carol is the visible recipient.",
            hoursAgo = 24
        ),

        // ============================================================================
        // Group G: TO + CC + BCC mixed with externals (3 emails) - 3 test users see these
        // ============================================================================
        TestEmail(
            id = "test-025",
            from = TestUsers.MILOS,
            to = "${TestUsers.DUSAN}, alice@example.com",
            cc = TestUsers.ARAMBA,
            bcc = "bob@example.com",
            subject = "[FULL] M→D,Alice cc:A bcc:Bob",
            content = "Email with all recipient types including externals.\n\nTO: Dusan, Alice\nCC: Aramba\nBCC: Bob (external)\n\nAll three test users should see this.",
            hoursAgo = 25
        ),
        TestEmail(
            id = "test-026",
            from = TestUsers.DUSAN,
            to = "${TestUsers.ARAMBA}, bob@example.com",
            cc = TestUsers.MILOS,
            bcc = "alice@example.com",
            subject = "[FULL] D→A,Bob cc:M bcc:Alice",
            content = "Email with all recipient types including externals.\n\nTO: Aramba, Bob\nCC: Milos\nBCC: Alice (external)\n\nAll three test users should see this.",
            hoursAgo = 26
        ),
        TestEmail(
            id = "test-027",
            from = TestUsers.ARAMBA,
            to = "${TestUsers.MILOS}, carol@example.com",
            cc = TestUsers.DUSAN,
            bcc = "bob@example.com",
            subject = "[FULL] A→M,Carol cc:D bcc:Bob",
            content = "Email with all recipient types including externals.\n\nTO: Milos, Carol\nCC: Dusan\nBCC: Bob (external)\n\nAll three test users should see this.",
            hoursAgo = 27
        ),

        // ============================================================================
        // Group H: External TO with test user BCC (3 emails)
        // Test users get BCC'd on external conversations
        // ============================================================================
        TestEmail(
            id = "test-028",
            from = TestUsers.MILOS,
            to = "alice@example.com, bob@example.com",
            bcc = TestUsers.DUSAN,
            subject = "[TO:2,BCC:1] M→Alice,Bob bcc:D",
            content = "Email from Milos to Alice and Bob, with Dusan as BCC.\n\nDusan can see this via BCC. Only Milos and Dusan (as test users) see this.",
            hoursAgo = 28
        ),
        TestEmail(
            id = "test-029",
            from = TestUsers.DUSAN,
            to = "bob@example.com, carol@example.com",
            bcc = TestUsers.ARAMBA,
            subject = "[TO:2,BCC:1] D→Bob,Carol bcc:A",
            content = "Email from Dusan to Bob and Carol, with Aramba as BCC.\n\nAramba can see this via BCC. Only Dusan and Aramba (as test users) see this.",
            hoursAgo = 29
        ),
        TestEmail(
            id = "test-030",
            from = TestUsers.ARAMBA,
            to = "alice@example.com, carol@example.com",
            bcc = TestUsers.MILOS,
            subject = "[TO:2,BCC:1] A→Alice,Carol bcc:M",
            content = "Email from Aramba to Alice and Carol, with Milos as BCC.\n\nMilos can see this via BCC. Only Aramba and Milos (as test users) see this.",
            hoursAgo = 30
        ),

        // ============================================================================
        // Group I: Reply Chain - 3 users (4 emails)
        // ============================================================================
        TestEmail(
            id = "test-031",
            from = TestUsers.MILOS,
            to = "${TestUsers.DUSAN}, ${TestUsers.ARAMBA}",
            subject = "[CHAIN-1] M→D,A",
            content = "Starting a group conversation with all three team members.\n\nLet's discuss the project.",
            hoursAgo = 4
        ),
        TestEmail(
            id = "test-032",
            from = TestUsers.DUSAN,
            to = TestUsers.MILOS,
            cc = TestUsers.ARAMBA,
            subject = "[CHAIN-2] Re: D→M cc:A",
            content = "Reply to the group conversation.\n\nI agree with the proposed approach.",
            hoursAgo = 3
        ),
        TestEmail(
            id = "test-033",
            from = TestUsers.ARAMBA,
            to = "${TestUsers.MILOS}, ${TestUsers.DUSAN}",
            subject = "[CHAIN-3] Re:Re: A→M,D",
            content = "Second reply in the chain.\n\nAdding my thoughts on the project timeline.",
            hoursAgo = 2
        ),
        TestEmail(
            id = "test-034",
            from = TestUsers.MILOS,
            to = TestUsers.DUSAN,
            cc = TestUsers.ARAMBA,
            subject = "[CHAIN-4] Re:Re:Re: M→D cc:A",
            content = "Final reply in the chain.\n\nLet's schedule a meeting to finalize.",
            hoursAgo = 1
        ),

        // ============================================================================
        // Group J: Reply Chain - 2 users only (4 emails) - ARAMBA excluded
        // Tests: Aramba should NOT see this conversation
        // ============================================================================
        TestEmail(
            id = "test-035",
            from = TestUsers.MILOS,
            to = TestUsers.DUSAN,
            subject = "[PRIVATE-1] M→D only",
            content = "Starting a PRIVATE conversation between Milos and Dusan.\n\nAramba should NOT see this entire thread.",
            hoursAgo = 8
        ),
        TestEmail(
            id = "test-036",
            from = TestUsers.DUSAN,
            to = TestUsers.MILOS,
            subject = "[PRIVATE-2] Re: D→M only",
            content = "Reply to the private conversation.\n\nThis is just between us.",
            hoursAgo = 7
        ),
        TestEmail(
            id = "test-037",
            from = TestUsers.MILOS,
            to = TestUsers.DUSAN,
            subject = "[PRIVATE-3] Re:Re: M→D only",
            content = "Continuing our private discussion.\n\nStill just between Milos and Dusan.",
            hoursAgo = 6
        ),
        TestEmail(
            id = "test-038",
            from = TestUsers.DUSAN,
            to = TestUsers.MILOS,
            subject = "[PRIVATE-4] Re:Re:Re: D→M only",
            content = "Final message in our private thread.\n\nAramba never sees any of these.",
            hoursAgo = 5
        ),

        // ============================================================================
        // Group K: Complex - TO(3) CC(2) BCC(1) (1 email)
        // ============================================================================
        TestEmail(
            id = "test-039",
            from = TestUsers.MILOS,
            to = "${TestUsers.DUSAN}, ${TestUsers.ARAMBA}",
            cc = "${TestUsers.DUSAN}, ${TestUsers.ARAMBA}",
            bcc = TestUsers.MILOS,
            subject = "[MAX] TO:2,CC:2,BCC:1",
            content = "Complex email with maximum recipient types.\n\nTO: Dusan, Aramba\nCC: Dusan, Aramba\nBCC: Milos\n\nAll three users should see this.",
            hoursAgo = 0
        )
    )

    /**
     * Returns the metadata assignments for a given user email.
     * Based on email visibility rules and per-user customization.
     *
     * Visibility Summary (39 emails total):
     * - MILOS (33): NOT visible = test-002, test-011, test-012 (D↔A private), test-023, test-029 (BCC'd to others)
     * - DUSAN (33): NOT visible = test-003, test-009, test-010 (M↔A private), test-024, test-030 (BCC'd to others)
     * - ARAMBA (27): NOT visible = test-001, test-007, test-008 (M↔D private), test-035-038 (M↔D chain), test-022, test-028 (BCC'd to others)
     */
    private fun getMetadataForUser(userEmail: String): List<TestMetadata> {
        return when (userEmail.lowercase()) {
            TestUsers.MILOS.lowercase() -> listOf(
                // MILOS sees 33 emails
                // NOT visible: test-002 (D→A), test-011 (D→A), test-012 (A→D), test-023 (D→Bob bcc:A), test-029 (D→Bob,Carol bcc:A)
                // Group A (test-001, test-003 only - NOT test-002)
                TestMetadata("test-001", isRead = true, isStarred = false),
                TestMetadata("test-003", isRead = false, isStarred = false), // UNREAD
                // Group B (all 3)
                TestMetadata("test-004", isRead = true, isStarred = false),
                TestMetadata("test-005", isRead = true, isStarred = false),
                TestMetadata("test-006", isRead = true, isStarred = false),
                // Group C (M↔D and M↔A only - NOT test-011, test-012)
                TestMetadata("test-007", isRead = true, isStarred = false),
                TestMetadata("test-008", isRead = true, isStarred = false),
                TestMetadata("test-009", isRead = true, isStarred = false),
                TestMetadata("test-010", isRead = true, isStarred = false),
                // Group D (all 6)
                TestMetadata("test-013", isRead = true, isStarred = true), // STARRED
                TestMetadata("test-014", isRead = true, isStarred = false),
                TestMetadata("test-015", isRead = true, isStarred = false),
                TestMetadata("test-016", isRead = true, isStarred = false),
                TestMetadata("test-017", isRead = true, isStarred = false),
                TestMetadata("test-018", isRead = true, isStarred = false),
                // Group E (all 3)
                TestMetadata("test-019", isRead = true, isStarred = false),
                TestMetadata("test-020", isRead = true, isStarred = false),
                TestMetadata("test-021", isRead = true, isStarred = false),
                // Group F (test-022, test-024 only - NOT test-023 which is D→Bob bcc:A)
                TestMetadata("test-022", isRead = true, isStarred = false), // M→Alice bcc:D - M sees as sender
                TestMetadata("test-024", isRead = true, isStarred = false), // A→Carol bcc:M - M sees as BCC
                // Group G (all 3)
                TestMetadata("test-025", isRead = true, isStarred = false, tags = listOf(Tags.WORK, Tags.IMPORTANT)), // TAGGED
                TestMetadata("test-026", isRead = true, isStarred = false),
                TestMetadata("test-027", isRead = true, isStarred = false),
                // Group H (test-028, test-030 only - NOT test-029 which is D→Bob,Carol bcc:A)
                TestMetadata("test-028", isRead = true, isStarred = false), // M→Alice,Bob bcc:D - M sees as sender
                TestMetadata("test-030", isRead = true, isStarred = false), // A→Alice,Carol bcc:M - M sees as BCC
                // Group I (all 4)
                TestMetadata("test-031", isRead = false, isStarred = false), // UNREAD
                TestMetadata("test-032", isRead = true, isStarred = false),
                TestMetadata("test-033", isRead = true, isStarred = false),
                TestMetadata("test-034", isRead = true, isStarred = false),
                // Group J - private M↔D (all 4)
                TestMetadata("test-035", isRead = true, isStarred = true), // STARRED
                TestMetadata("test-036", isRead = true, isStarred = false),
                TestMetadata("test-037", isRead = true, isStarred = false),
                TestMetadata("test-038", isRead = true, isStarred = false),
                // Group K
                TestMetadata("test-039", isRead = true, isStarred = false)
            )

            TestUsers.DUSAN.lowercase() -> listOf(
                // DUSAN sees 33 emails
                // NOT visible: test-003 (A→M), test-009 (M→A), test-010 (A→M), test-024 (A→Carol bcc:M), test-030 (A→Alice,Carol bcc:M)
                // Group A (test-001, test-002 only - NOT test-003)
                TestMetadata("test-001", isRead = false, isStarred = false), // UNREAD
                TestMetadata("test-002", isRead = true, isStarred = false),
                // Group B (all 3)
                TestMetadata("test-004", isRead = true, isStarred = false),
                TestMetadata("test-005", isRead = true, isStarred = false),
                TestMetadata("test-006", isRead = true, isStarred = false),
                // Group C (M↔D and D↔A only - NOT test-009, test-010)
                TestMetadata("test-007", isRead = true, isStarred = true), // STARRED
                TestMetadata("test-008", isRead = true, isStarred = false),
                TestMetadata("test-011", isRead = true, isStarred = false),
                TestMetadata("test-012", isRead = true, isStarred = false),
                // Group D (all 6)
                TestMetadata("test-013", isRead = true, isStarred = false),
                TestMetadata("test-014", isRead = true, isStarred = false),
                TestMetadata("test-015", isRead = true, isStarred = false),
                TestMetadata("test-016", isRead = true, isStarred = false),
                TestMetadata("test-017", isRead = true, isStarred = false),
                TestMetadata("test-018", isRead = true, isStarred = false),
                // Group E (all 3)
                TestMetadata("test-019", isRead = true, isStarred = true), // STARRED
                TestMetadata("test-020", isRead = true, isStarred = false),
                TestMetadata("test-021", isRead = true, isStarred = false),
                // Group F (test-022, test-023 only - NOT test-024 which is A→Carol bcc:M)
                TestMetadata("test-022", isRead = true, isStarred = false), // M→Alice bcc:D - D sees as BCC
                TestMetadata("test-023", isRead = true, isStarred = false), // D→Bob bcc:A - D sees as sender
                // Group G (all 3)
                TestMetadata("test-025", isRead = true, isStarred = false),
                TestMetadata("test-026", isRead = true, isStarred = false, tags = listOf(Tags.URGENT)), // TAGGED
                TestMetadata("test-027", isRead = true, isStarred = false),
                // Group H (test-028, test-029 only - NOT test-030 which is A→Alice,Carol bcc:M)
                TestMetadata("test-028", isRead = true, isStarred = false), // M→Alice,Bob bcc:D - D sees as BCC
                TestMetadata("test-029", isRead = true, isStarred = false), // D→Bob,Carol bcc:A - D sees as sender
                // Group I (all 4)
                TestMetadata("test-031", isRead = true, isStarred = false),
                TestMetadata("test-032", isRead = false, isStarred = false), // UNREAD
                TestMetadata("test-033", isRead = true, isStarred = false),
                TestMetadata("test-034", isRead = true, isStarred = false),
                // Group J - private M↔D (all 4)
                TestMetadata("test-035", isRead = true, isStarred = false),
                TestMetadata("test-036", isRead = true, isStarred = false),
                TestMetadata("test-037", isRead = true, isStarred = false),
                TestMetadata("test-038", isRead = true, isStarred = false),
                // Group K
                TestMetadata("test-039", isRead = true, isStarred = false)
            )

            TestUsers.ARAMBA.lowercase() -> listOf(
                // ARAMBA sees 27 emails
                // NOT visible: test-001 (M→D), test-007 (M→D), test-008 (D→M), test-022 (M→Alice bcc:D),
                //              test-028 (M→Alice,Bob bcc:D), test-035-038 (M↔D private chain)
                // Group A (test-002, test-003 only - NOT test-001)
                TestMetadata("test-002", isRead = false, isStarred = false), // UNREAD
                TestMetadata("test-003", isRead = true, isStarred = false),
                // Group B (all 3)
                TestMetadata("test-004", isRead = true, isStarred = false),
                TestMetadata("test-005", isRead = true, isStarred = false),
                TestMetadata("test-006", isRead = true, isStarred = false),
                // Group C (M↔A and D↔A only - NOT test-007, test-008)
                TestMetadata("test-009", isRead = true, isStarred = false),
                TestMetadata("test-010", isRead = true, isStarred = false),
                TestMetadata("test-011", isRead = true, isStarred = false),
                TestMetadata("test-012", isRead = true, isStarred = false),
                // Group D (all 6)
                TestMetadata("test-013", isRead = true, isStarred = false),
                TestMetadata("test-014", isRead = true, isStarred = true), // STARRED
                TestMetadata("test-015", isRead = true, isStarred = false),
                TestMetadata("test-016", isRead = true, isStarred = false),
                TestMetadata("test-017", isRead = true, isStarred = false),
                TestMetadata("test-018", isRead = true, isStarred = false),
                // Group E (all 3)
                TestMetadata("test-019", isRead = true, isStarred = false),
                TestMetadata("test-020", isRead = true, isStarred = false),
                TestMetadata("test-021", isRead = true, isStarred = false),
                // Group F (test-023, test-024 only - NOT test-022 which is M→Alice bcc:D)
                TestMetadata("test-023", isRead = true, isStarred = true), // STARRED - D→Bob bcc:A - A sees as BCC
                TestMetadata("test-024", isRead = true, isStarred = false), // A→Carol bcc:M - A sees as sender
                // Group G (all 3)
                TestMetadata("test-025", isRead = true, isStarred = false),
                TestMetadata("test-026", isRead = true, isStarred = false),
                TestMetadata("test-027", isRead = true, isStarred = false, tags = listOf(Tags.PERSONAL, Tags.FINANCE)), // TAGGED
                // Group H (test-029, test-030 only - NOT test-028 which is M→Alice,Bob bcc:D)
                TestMetadata("test-029", isRead = true, isStarred = false), // D→Bob,Carol bcc:A - A sees as BCC
                TestMetadata("test-030", isRead = true, isStarred = false), // A→Alice,Carol bcc:M - A sees as sender
                // Group I (all 4)
                TestMetadata("test-031", isRead = true, isStarred = false),
                TestMetadata("test-032", isRead = true, isStarred = false),
                TestMetadata("test-033", isRead = false, isStarred = false), // UNREAD
                TestMetadata("test-034", isRead = true, isStarred = false),
                // Group J - NOT included (M↔D private - ARAMBA excluded)
                // Group K
                TestMetadata("test-039", isRead = true, isStarred = false)
            )

            else -> emptyList()
        }
    }

    /**
     * Clears all test data including emails and metadata for all users.
     */
    suspend fun clearAllTestData(): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        return try {
            Log.d(TAG, "Starting clearAllTestData for user: $userId")

            // Delete all 39 test emails from allMessages collection
            val allMessagesCollection = firestore.collection(Collections.ALL_MESSAGES)
            val testEmailIds = (1..39).map { "test-${it.toString().padStart(3, '0')}" }

            testEmailIds.forEach { emailId ->
                try {
                    allMessagesCollection.document(emailId).delete().await()
                    Log.d(TAG, "Deleted test email: $emailId")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not delete $emailId (may not exist): ${e.message}")
                }
            }
            Log.d(TAG, "Deleted ${testEmailIds.size} test emails from allMessages")

            // Delete metadata for all 3 test users
            val userIds = mapOf(
                TestUsers.MILOS to getUserIdByEmail(TestUsers.MILOS),
                TestUsers.DUSAN to getUserIdByEmail(TestUsers.DUSAN),
                TestUsers.ARAMBA to getUserIdByEmail(TestUsers.ARAMBA)
            )

            userIds.forEach { (email, foundUserId) ->
                if (foundUserId != null) {
                    try {
                        val metadataCollection = firestore.collection(Collections.USERS)
                            .document(foundUserId)
                            .collection(Collections.MESSAGES_METADATA)

                        val metadata = metadataCollection.get().await()
                        metadata.documents.forEach { doc ->
                            doc.reference.delete().await()
                        }
                        Log.d(TAG, "Deleted ${metadata.size()} metadata documents for $email")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete metadata for $email: ${e.message}")
                    }
                }
            }

            // Also clean up legacy test emails (email-001 to email-006)
            val legacyEmailIds = listOf("email-001", "email-002", "email-003", "email-004", "email-005", "email-006")
            legacyEmailIds.forEach { emailId ->
                try {
                    allMessagesCollection.document(emailId).delete().await()
                } catch (_: Exception) {
                    // Ignore if doesn't exist
                }
            }
            Log.d(TAG, "Cleaned up legacy test emails")

            Log.d(TAG, "All test data cleared successfully")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear test data: ${e.message}", e)
            Result.failure(e)
        }
    }

}
