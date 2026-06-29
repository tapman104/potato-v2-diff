package com.tapman104.mpvplayer.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.subtitleDataStore by preferencesDataStore(name = "subtitle_prefs")

object SubtitlePreferences {
    val SIZE_KEY = floatPreferencesKey("subtitle_size")
    val POSITION_KEY = floatPreferencesKey("subtitle_position")

    const val DEFAULT_SIZE = 1.1f
    const val DEFAULT_POSITION = 0.07f

    fun sizeFlow(context: Context): Flow<Float> =
        context.subtitleDataStore.data.map { it[SIZE_KEY] ?: DEFAULT_SIZE }

    fun positionFlow(context: Context): Flow<Float> =
        context.subtitleDataStore.data.map { it[POSITION_KEY] ?: DEFAULT_POSITION }

    suspend fun save(context: Context, size: Float, position: Float) {
        context.subtitleDataStore.edit { prefs ->
            prefs[SIZE_KEY] = size
            prefs[POSITION_KEY] = position
        }
    }

    suspend fun reset(context: Context) {
        context.subtitleDataStore.edit { prefs ->
            prefs[SIZE_KEY] = DEFAULT_SIZE
            prefs[POSITION_KEY] = DEFAULT_POSITION
        }
    }
}
