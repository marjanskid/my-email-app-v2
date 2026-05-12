package com.example.myemailapp.domain.exception

sealed class EmailException(message: String) : Exception(message) {
    class NotFound(emailId: String) : EmailException("Email not found: $emailId")
    class InvalidData(reason: String) : EmailException("Invalid email data: $reason")
    class NotAuthenticated : EmailException("User not authenticated")
}
