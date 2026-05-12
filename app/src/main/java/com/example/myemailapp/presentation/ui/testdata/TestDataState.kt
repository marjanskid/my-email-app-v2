package com.example.myemailapp.presentation.ui.testdata

data class TestDataState(
    val seedingState: SeedingState = SeedingState.Idle,
    val clearingState: ClearingState = ClearingState.Idle,
    val testEmailId: String = ""
)

sealed class SeedingState {
    data object Idle : SeedingState()
    data object Seeding : SeedingState()
    data object Success : SeedingState()
    data class Error(val message: String) : SeedingState()
}

sealed class ClearingState {
    data object Idle : ClearingState()
    data object Clearing : ClearingState()
    data object Success : ClearingState()
    data class Error(val message: String) : ClearingState()
}
