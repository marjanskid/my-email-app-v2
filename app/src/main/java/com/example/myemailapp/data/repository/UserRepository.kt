package com.example.myemailapp.data.repository

import com.example.myemailapp.domain.model.User

interface UserRepository {
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun getUserProfile(): Result<User>
    suspend fun updateUserProfile(user: User): Result<Unit>
    suspend fun userProfileExists(): Result<Boolean>
}
