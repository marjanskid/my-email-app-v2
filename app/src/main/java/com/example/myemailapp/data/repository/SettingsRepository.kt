package com.example.myemailapp.data.repository

import com.example.myemailapp.domain.model.settings.RefreshInterval
import com.example.myemailapp.domain.model.settings.SortOrder
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val refreshInterval: Flow<RefreshInterval>
    val sortOrder: Flow<SortOrder>

    suspend fun setRefreshInterval(interval: RefreshInterval)
    suspend fun setSortOrder(order: SortOrder)
}
