package com.example.myemailapp.data.repository

import com.example.myemailapp.data.constants.FirestoreFirebaseConstants.Collections
import com.example.myemailapp.data.mapper.toDomain
import com.example.myemailapp.data.mapper.toDto
import com.example.myemailapp.data.model.UserDto
import com.example.myemailapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : UserRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userDocument() =
        db.collection(Collections.USERS)
            .document(currentUserId)

    override suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            val userDto = user.copy(userId = currentUserId).toDto()

            userDocument()
                .set(userDto)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(): Result<User> {
        return try {
            val document = userDocument()
                .get()
                .await()

            if (!document.exists()) {
                return Result.failure(NoSuchElementException("User profile not found"))
            }

            val userDto = document.toObject<UserDto>()
                ?: return Result.failure(IllegalStateException("Failed to parse user profile"))

            Result.success(userDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            val userDto = user.toDto()

            userDocument()
                .set(userDto)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun userProfileExists(): Result<Boolean> {
        return try {
            val document = userDocument()
                .get()
                .await()

            Result.success(document.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
