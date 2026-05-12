package com.example.myemailapp.data.service

import com.example.myemailapp.domain.model.EmailResult
import kotlinx.coroutines.flow.SharedFlow

interface EmailStatusService {
    val emailStatusEvents: SharedFlow<EmailResult>
    suspend fun emitStatus(result: EmailResult)
}
