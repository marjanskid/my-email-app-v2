package com.example.myemailapp.data.mapper

import com.example.myemailapp.data.model.UserDto
import com.example.myemailapp.domain.model.User
import com.google.firebase.Timestamp

fun UserDto.toDomain(): User = User(
    userId = userId,
    displayName = displayName,
    email = email
)

fun User.toDto(): UserDto = UserDto(
    userId = userId,
    displayName = displayName,
    email = email,
    createdAt = Timestamp.now()
)
