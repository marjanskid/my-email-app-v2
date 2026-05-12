package com.example.myemailapp.domain.model

enum class ProcessState {
    Initial, Loading, Success, Failure
}

val ProcessState.isLoading: Boolean
    get() = this == ProcessState.Loading

val ProcessState.isFailure: Boolean
    get() = this == ProcessState.Failure

val ProcessState.isSuccess: Boolean
    get() = this == ProcessState.Success