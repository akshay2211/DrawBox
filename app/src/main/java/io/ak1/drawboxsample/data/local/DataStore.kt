package io.ak1.drawboxsample.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

/**
 * Created by akshay on 28/12/21
 * https://ak1.io
 */
// DataStore Instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Preference Keys
val themePreferenceKey = intPreferencesKey("list_theme")




// Retrieving functions
/**
 * extension [isDarkThemeOn] checks the saved theme from preference
 * and returns boolean
 */
fun Context.isDarkThemeOn() = dataStore.data
    .map { preferences ->
        // No type safety.
        preferences[themePreferenceKey] ?: 0
    }