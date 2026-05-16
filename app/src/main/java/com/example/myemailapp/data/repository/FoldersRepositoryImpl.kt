package com.example.myemailapp.data.repository

import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Collections
import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Field
import com.example.myemailapp.data.mapper.toDomain
import com.example.myemailapp.data.mapper.toDto
import com.example.myemailapp.data.model.FolderDto
import com.example.myemailapp.domain.model.Folder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class FoldersRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : FoldersRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userFoldersCollection() =
        db.collection(Collections.USERS)
            .document(currentUserId)
            .collection(Collections.FOLDERS)

    override suspend fun getFolders(): Result<List<Folder>> {
        return try {
            val documents = userFoldersCollection()
                .orderBy(Field.NAME, Query.Direction.ASCENDING)
                .get()
                .await()

            val folders = documents.mapNotNull { doc ->
                doc.toObject<FolderDto>().toDomain()
            }
            Result.success(folders)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(folder: Folder): Result<String> {
        return try {
            val folderId = folder.id.ifEmpty { UUID.randomUUID().toString() }
            val folderDto = folder.copy(id = folderId).toDto()

            userFoldersCollection()
                .document(folderId)
                .set(folderDto)
                .await()

            Result.success(folderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> {
        return try {
            val folderDto = folder.toDto()

            userFoldersCollection()
                .document(folder.id)
                .set(folderDto)
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFolder(folderId: String): Result<Unit> {
        return try {
            userFoldersCollection()
                .document(folderId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFolderById(folderId: String): Result<Folder> {
        return try {
            val document = userFoldersCollection()
                .document(folderId)
                .get()
                .await()

            if (!document.exists()) {
                return Result.failure(NoSuchElementException("Folder not found: $folderId"))
            }

            val folderDto = document.toObject<FolderDto>()
                ?: return Result.failure(IllegalStateException("Failed to parse folder"))

            Result.success(folderDto.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChildFolders(parentId: String): Result<List<Folder>> {
        return try {
            val documents = userFoldersCollection()
                .whereEqualTo(Field.PARENT_ID, parentId)
                .orderBy(Field.NAME, Query.Direction.ASCENDING)
                .get()
                .await()

            val folders = documents.mapNotNull { doc ->
                doc.toObject<FolderDto>().toDomain()
            }
            Result.success(folders)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}