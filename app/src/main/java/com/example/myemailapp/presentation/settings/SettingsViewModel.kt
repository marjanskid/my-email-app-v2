package com.example.myemailapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myemailapp.data.repository.SettingsRepository
import com.example.myemailapp.domain.model.settings.RefreshInterval
import com.example.myemailapp.domain.model.settings.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val refreshInterval = settingsRepository.refreshInterval.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RefreshInterval.MANUAL
    )

    val sortOrder = settingsRepository.sortOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortOrder.DESCENDING
    )

    private val _refreshIntervalExpanded = MutableStateFlow(false)
    val refreshIntervalExpanded = _refreshIntervalExpanded.asStateFlow()

    fun setRefreshInterval(interval: RefreshInterval) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setRefreshInterval(interval)
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setSortOrder(order)
        }
    }

    fun setRefreshIntervalExpanded(expanded: Boolean) {
        _refreshIntervalExpanded.value = expanded
    }
}
