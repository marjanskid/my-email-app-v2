package com.example.myemailapp.data.repository

import com.example.myemailapp.data.repository.AuthRepository
import com.example.myemailapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()

            Result.success(
                User(
                    userId = result.user?.uid ?: "",
                    displayName = result.user?.displayName ?: "",
                    email = result.user?.email ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override val currentUser: User?
        get() {
            val firebaseCurrentUser = auth.currentUser

            if (firebaseCurrentUser != null) {
                return User(
                    userId = firebaseCurrentUser.uid,
                    displayName = firebaseCurrentUser.displayName ?: "",
                    email = firebaseCurrentUser.email ?: ""
                )
            }

            return null
        }
}