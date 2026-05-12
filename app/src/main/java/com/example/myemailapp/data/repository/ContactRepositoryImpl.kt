package com.example.myemailapp.data.repository

import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Collections
import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Field
import com.example.myemailapp.data.mapper.toDomain
import com.example.myemailapp.data.mapper.toDto
import com.example.myemailapp.data.model.ContactDto
import com.example.myemailapp.domain.model.Contact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ContactRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ContactRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userContactsCollection() =
        db.collection(Collections.USERS)
            .document(currentUserId)
            .collection(Collections.CONTACTS)

    override suspend fun getContacts(): Result<List<Contact>> {
        return try {
            val documents = userContactsCollection()
                .orderBy(Field.NAME, Query.Direction.ASCENDING)
                .get()
                .await()

            val contacts = documents.mapNotNull { doc ->
                doc.toObject<ContactDto>().toDomain()
            }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addContact(contact: Contact): Result<String> {
        return try {
            val contactId = contact.id.ifEmpty { UUID.randomUUID().toString() }
            val contactDto = contact.copy(id = contactId).toDto()

            userContactsCollection()
                .document(contactId)
                .set(contactDto)
                .await()

            Result.success(contactId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContact(contact: Contact): Result<Unit> {
        return try {
            val contactDto = contact.toDto()

            userContactsCollection()
                .document(contact.id)
                .set(contactDto)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(contactId: String): Result<Unit> {
        return try {
            userContactsCollection()
                .document(contactId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactById(contactId: String): Result<Contact> {
        return try {
            val document = userContactsCollection()
                .document(contactId)
                .get()
                .await()

            if (!document.exists()) {
                return Result.failure(NoSuchElementException("Contact not found: $contactId"))
            }

            val contactDto = document.toObject<ContactDto>()
                ?: return Result.failure(IllegalStateException("Failed to parse contact"))

            Result.success(contactDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}