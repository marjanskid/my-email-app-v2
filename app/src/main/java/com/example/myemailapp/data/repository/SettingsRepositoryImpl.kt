package com.example.myemailapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.myemailapp.domain.model.settings.RefreshInterval
import com.example.myemailapp.domain.model.settings.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val REFRESH_INTERVAL_KEY = stringPreferencesKey("refresh_interval")
        private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
    }

    override val refreshInterval: Flow<RefreshInterval> = dataStore.data.map { preferences ->
        val intervalName = preferences[REFRESH_INTERVAL_KEY] ?: RefreshInterval.MANUAL.name
        try {
            RefreshInterval.valueOf(intervalName)
        } catch (_: IllegalArgumentException) {
            RefreshInterval.MANUAL
        }
    }

    override val sortOrder: Flow<SortOrder> = dataStore.data.map { preferences ->
        val orderName = preferences[SORT_ORDER_KEY] ?: SortOrder.DESCENDING.name
        try {
            SortOrder.valueOf(orderName)
        } catch (_: IllegalArgumentException) {
            SortOrder.DESCENDING
        }
    }

    override suspend fun setRefreshInterval(interval: RefreshInterval) {
        dataStore.edit { preferences ->
            preferences[REFRESH_INTERVAL_KEY] = interval.name
        }
    }

    override suspend fun setSortOrder(order: SortOrder) {
        dataStore.edit { preferences ->
            preferences[SORT_ORDER_KEY] = order.name
        }
    }
}
