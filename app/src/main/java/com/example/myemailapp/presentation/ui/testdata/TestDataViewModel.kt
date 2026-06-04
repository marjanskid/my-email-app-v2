package com.example.myemailapp.presentation.ui.testdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.util.TestDataSeeder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TestDataViewModel(
    private val testDataSeeder: TestDataSeeder
) : ViewModel() {

    private val _state = MutableStateFlow(TestDataState())
    val state = _state.asStateFlow()

    fun seedTestData() {
        viewModelScope.launch {
            _state.update { it.copy(seedingState = SeedingState.Seeding) }
            testDataSeeder.seedTestData()
                .onSuccess {
                    _state.update { it.copy(seedingState = SeedingState.Success) }
                }
                .onFailure { error ->
                    _state.update { it.copy(seedingState = SeedingState.Error(error.message ?: "Unknown error")) }
                }
        }
    }

    fun resetSeedingState() {
        _state.update { it.copy(seedingState = SeedingState.Idle) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _state.update { it.copy(clearingState = ClearingState.Clearing) }
            testDataSeeder.clearAllData()
                .onSuccess {
                    _state.update { it.copy(clearingState = ClearingState.Success) }
                }
                .onFailure { error ->
                    _state.update { it.copy(clearingState = ClearingState.Error(error.message ?: "Unknown error")) }
                }
        }
    }

    fun resetClearingState() {
        _state.update { it.copy(clearingState = ClearingState.Idle) }
    }
}
