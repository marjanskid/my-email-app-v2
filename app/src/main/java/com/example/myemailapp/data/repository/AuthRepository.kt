package com.example.myemailapp.data.repository

import com.example.myemailapp.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    val currentUser: User?
}