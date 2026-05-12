package com.example.myemailapp.data.repository

import com.example.myemailapp.domain.model.Contact

interface ContactRepository {
    suspend fun getContacts(): Result<List<Contact>>
    suspend fun addContact(contact: Contact): Result<String>
    suspend fun updateContact(contact: Contact): Result<Unit>
    suspend fun deleteContact(contactId: String): Result<Unit>
    suspend fun getContactById(contactId: String): Result<Contact>
}
