package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lobo_prefs")

class PreferencesRepository(private val context: Context) {
    private val mutedKey = booleanPreferencesKey("feed_muted")
    private val autoPlayKey = booleanPreferencesKey("feed_autoplay")

    val isMutedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[mutedKey] ?: false
    }

    val isAutoPlayFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoPlayKey] ?: true
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[mutedKey] = muted
        }
    }

    suspend fun setAutoPlay(autoPlay: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[autoPlayKey] = autoPlay
        }
    }
}
