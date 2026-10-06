package com.poojan.pathkeeper

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pathkeeper_prefs")

class PrefsRepository(private val context: Context) {

    private object Keys {
        val FOLDER_URI = stringPreferencesKey("folder_uri")
        val HOUR = intPreferencesKey("hour")
        val MINUTE = intPreferencesKey("minute")
        val RECENT_URIS = stringSetPreferencesKey("recent_uris")
        val CURRENT_URI = stringPreferencesKey("current_uri")
        val LAST_CHANGED = longPreferencesKey("last_changed")
    }

    val folderUri: Flow<String?> = context.dataStore.data.map { it[Keys.FOLDER_URI] }
    val scheduleHour: Flow<Int> = context.dataStore.data.map { it[Keys.HOUR] ?: 6 }
    val scheduleMinute: Flow<Int> = context.dataStore.data.map { it[Keys.MINUTE] ?: 0 }
    val currentWallpaperUri: Flow<String?> = context.dataStore.data.map { it[Keys.CURRENT_URI] }
    val lastChangedAt: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_CHANGED] ?: 0L }

    suspend fun setFolderUri(uri: String) {
        context.dataStore.edit { it[Keys.FOLDER_URI] = uri }
    }

    suspend fun setSchedule(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.HOUR] = hour
            it[Keys.MINUTE] = minute
        }
    }

    suspend fun getRecentUris(): Set<String> {
        return context.dataStore.data.first()[Keys.RECENT_URIS] ?: emptySet()
    }

    suspend fun getFolderUri(): String? {
        return context.dataStore.data.first()[Keys.FOLDER_URI]
    }

    suspend fun getScheduleHour(): Int {
        return context.dataStore.data.first()[Keys.HOUR] ?: 6
    }

    suspend fun getScheduleMinute(): Int {
        return context.dataStore.data.first()[Keys.MINUTE] ?: 0
    }

    suspend fun recordShown(uri: String, maxHistory: Int = 5) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.RECENT_URIS] ?: emptySet()).toMutableList()
            current.add(uri)
            while (current.size > maxHistory) current.removeAt(0)
            prefs[Keys.RECENT_URIS] = current.toSet()
            prefs[Keys.CURRENT_URI] = uri
            prefs[Keys.LAST_CHANGED] = System.currentTimeMillis()
        }
    }
}
